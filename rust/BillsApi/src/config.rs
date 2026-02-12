use std::env;

#[derive(Clone, Debug)]
pub struct AppConfig {
    pub port: u16,
    pub postgres: PostgresConfig,
    pub rabbitmq: RabbitMqConfig,
}

#[derive(Clone, Debug)]
pub struct PostgresConfig {
    pub host: String,
    pub port: u16,
    pub db: String,
    pub user: String,
    pub password: String,
    pub max_pool_size: u32,
    pub min_idle: u32,
}

#[derive(Clone, Debug)]
pub struct RabbitMqConfig {
    pub host: String,
    pub port: u16,
    pub user: String,
    pub password: String,
    pub vhost: String,
    pub bill_created_queue: String,
}

impl AppConfig {
    pub fn from_env() -> Self {
        Self {
            port: env_u16("PORT", 8080),
            postgres: PostgresConfig {
                host: env_string("POSTGRES_HOST", "localhost"),
                port: env_u16("POSTGRES_PORT", 5440),
                db: env_string("POSTGRES_DB", "api_lang_arena"),
                user: env_string("POSTGRES_USER", "api_lang_user"),
                password: env_string("POSTGRES_PASSWORD", "api_lang_password"),
                max_pool_size: env_u32("RUST_DB_MAX_POOL_SIZE", 6),
                min_idle: env_u32("RUST_DB_MIN_IDLE", 1),
            },
            rabbitmq: RabbitMqConfig {
                host: env_string("RABBITMQ_HOST", "localhost"),
                port: env_u16("RABBITMQ_PORT", 5672),
                user: env_string("RABBITMQ_USER", "guest"),
                password: env_string("RABBITMQ_PASSWORD", "guest"),
                vhost: env_string("RABBITMQ_VHOST", "/"),
                bill_created_queue: env_string("RABBITMQ_BILL_CREATED_QUEUE", "bill-created"),
            },
        }
    }
}

impl PostgresConfig {
    pub fn database_url(&self) -> String {
        format!(
            "postgres://{}:{}@{}:{}/{}",
            urlencoding::encode(&self.user),
            urlencoding::encode(&self.password),
            self.host,
            self.port,
            self.db
        )
    }
}

impl RabbitMqConfig {
    pub fn amqp_url(&self) -> String {
        let encoded_vhost = if self.vhost == "/" {
            "%2F".to_string()
        } else {
            urlencoding::encode(self.vhost.trim_start_matches('/')).to_string()
        };

        format!(
            "amqp://{}:{}@{}:{}/{}",
            urlencoding::encode(&self.user),
            urlencoding::encode(&self.password),
            self.host,
            self.port,
            encoded_vhost
        )
    }
}

fn env_string(name: &str, default: &str) -> String {
    env::var(name).unwrap_or_else(|_| default.to_string())
}

fn env_u16(name: &str, default: u16) -> u16 {
    env::var(name)
        .ok()
        .and_then(|v| v.parse::<u16>().ok())
        .unwrap_or(default)
}

fn env_u32(name: &str, default: u32) -> u32 {
    env::var(name)
        .ok()
        .and_then(|v| v.parse::<u32>().ok())
        .unwrap_or(default)
}
