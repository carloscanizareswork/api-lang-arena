mod app_error;
mod config;

mod domain {
    pub mod bills;
}

mod application {
    pub mod bills {
        pub mod create;
        pub mod get;
    }
}

mod infrastructure {
    pub mod messaging {
        pub mod rabbitmq_publisher;
    }

    pub mod persistence {
        pub mod entities {
            pub mod bill;
            pub mod bill_line;
        }

        pub mod repository;
    }
}

mod presentation {
    pub mod dto;
    pub mod handlers;
}

use crate::app_error::AppError;
use crate::application::bills::create::{BillWriteRepository, CreateBillService, IntegrationEventPublisher};
use crate::application::bills::get::{BillReadRepository, GetBillsService};
use crate::config::AppConfig;
use crate::infrastructure::messaging::rabbitmq_publisher::RabbitMqPublisher;
use crate::infrastructure::persistence::repository::SeaOrmBillRepository;
use crate::presentation::handlers::{router, AppState};
use sea_orm::{ConnectOptions, Database};
use sqlx::postgres::PgPoolOptions;
use std::net::SocketAddr;
use std::sync::Arc;
use std::time::Duration;
use tracing::info;

#[tokio::main]
async fn main() -> Result<(), AppError> {
    init_tracing();

    let config = AppConfig::from_env();
    let database_url = config.postgres.database_url();

    let minimal_pool = PgPoolOptions::new()
        .max_connections(config.postgres.max_pool_size)
        .min_connections(config.postgres.min_idle)
        .acquire_timeout(Duration::from_secs(5))
        .connect(&database_url)
        .await
        .map_err(|e| AppError::Infrastructure(format!("connect sqlx pool: {e}")))?;

    let mut sea_options = ConnectOptions::new(database_url.clone());
    sea_options.max_connections(config.postgres.max_pool_size);
    sea_options.min_connections(config.postgres.min_idle);
    sea_options.connect_timeout(Duration::from_secs(5));
    sea_options.sqlx_logging(false);

    let sea_connection = Database::connect(sea_options)
        .await
        .map_err(|e| AppError::Infrastructure(format!("connect seaorm: {e}")))?;

    let repository = Arc::new(SeaOrmBillRepository::new(sea_connection));

    let bill_read_repository: Arc<dyn BillReadRepository> = repository.clone();
    let bill_write_repository: Arc<dyn BillWriteRepository> = repository.clone();

    let event_publisher: Arc<dyn IntegrationEventPublisher> = Arc::new(
        RabbitMqPublisher::new(
            &config.rabbitmq.amqp_url(),
            &config.rabbitmq.bill_created_queue,
        )
        .await?,
    );

    let get_bills_service = Arc::new(GetBillsService::new(bill_read_repository));
    let create_bill_service = Arc::new(CreateBillService::new(
        bill_write_repository,
        event_publisher,
    ));

    let app_state = AppState {
        minimal_pool,
        get_bills_service,
        create_bill_service,
    };

    let app = router(app_state);

    let addr = SocketAddr::from(([0, 0, 0, 0], config.port));
    let listener = tokio::net::TcpListener::bind(addr)
        .await
        .map_err(|e| AppError::Infrastructure(format!("bind listener: {e}")))?;

    info!("rust bills api listening on {}", addr);
    axum::serve(listener, app)
        .await
        .map_err(|e| AppError::Infrastructure(format!("serve axum app: {e}")))?;

    Ok(())
}

fn init_tracing() {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "info,rust_bills_api=info".into()),
        )
        .init();
}
