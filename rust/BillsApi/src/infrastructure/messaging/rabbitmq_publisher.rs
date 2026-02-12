use crate::app_error::AppError;
use crate::application::bills::create::{BillCreatedEvent, IntegrationEventPublisher};
use async_trait::async_trait;
use lapin::options::{BasicPublishOptions, QueueDeclareOptions};
use lapin::types::FieldTable;
use lapin::{BasicProperties, Channel, Connection, ConnectionProperties};
use serde::Serialize;

pub struct RabbitMqPublisher {
    _connection: Connection,
    channel: Channel,
    queue_name: String,
}

#[derive(Debug, Serialize)]
struct IntegrationEnvelope<TPayload> {
    event_name: String,
    occurred_at_utc: chrono::DateTime<chrono::Utc>,
    payload: TPayload,
}

impl RabbitMqPublisher {
    pub async fn new(amqp_url: &str, queue_name: &str) -> Result<Self, AppError> {
        let connection = Connection::connect(amqp_url, ConnectionProperties::default())
            .await
            .map_err(|e| AppError::Messaging(e.to_string()))?;

        let channel = connection
            .create_channel()
            .await
            .map_err(|e| AppError::Messaging(e.to_string()))?;

        channel
            .queue_declare(
                queue_name,
                QueueDeclareOptions {
                    durable: true,
                    ..Default::default()
                },
                FieldTable::default(),
            )
            .await
            .map_err(|e| AppError::Messaging(e.to_string()))?;

        Ok(Self {
            _connection: connection,
            channel,
            queue_name: queue_name.to_string(),
        })
    }
}

#[async_trait]
impl IntegrationEventPublisher for RabbitMqPublisher {
    async fn publish_bill_created(&self, event: BillCreatedEvent) -> Result<(), AppError> {
        let envelope = IntegrationEnvelope {
            event_name: "bill.created".to_string(),
            occurred_at_utc: chrono::Utc::now(),
            payload: event,
        };

        let body = serde_json::to_vec(&envelope)
            .map_err(|e| AppError::Messaging(format!("serialize event: {e}")))?;

        let confirm = self
            .channel
            .basic_publish(
                "",
                &self.queue_name,
                BasicPublishOptions::default(),
                &body,
                BasicProperties::default()
                    .with_content_type("application/json".into())
                    .with_delivery_mode(2),
            )
            .await
            .map_err(|e| AppError::Messaging(e.to_string()))?;

        confirm
            .await
            .map_err(|e| AppError::Messaging(e.to_string()))?;

        Ok(())
    }
}
