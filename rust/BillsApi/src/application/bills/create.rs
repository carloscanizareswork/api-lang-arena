use crate::app_error::AppError;
use crate::domain::bills::{NewBill, NewBillLine};
use async_trait::async_trait;
use chrono::{DateTime, NaiveDate, Utc};
use rust_decimal::Decimal;
use serde::Serialize;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct CreateBillLineCommand {
    pub concept: String,
    pub quantity: Decimal,
    pub unit_amount: Decimal,
}

#[derive(Debug, Clone)]
pub struct CreateBillCommand {
    pub bill_number: String,
    pub issued_at: NaiveDate,
    pub customer_name: String,
    pub currency: String,
    pub tax: Decimal,
    pub lines: Vec<CreateBillLineCommand>,
}

#[derive(Debug, Clone)]
pub struct CreateBillResult {
    pub id: i64,
    pub bill_number: String,
    pub issued_at: NaiveDate,
    pub subtotal: Decimal,
    pub tax: Decimal,
    pub total: Decimal,
    pub currency: String,
}

#[derive(Debug, Clone, Serialize)]
pub struct BillCreatedEvent {
    pub bill_id: i64,
    pub bill_number: String,
    pub issued_at: NaiveDate,
    pub subtotal: Decimal,
    pub tax: Decimal,
    pub total: Decimal,
    pub currency: String,
    pub occurred_at_utc: DateTime<Utc>,
    pub source: String,
}

#[async_trait]
pub trait BillWriteRepository: Send + Sync {
    async fn exists_by_bill_number(&self, bill_number: &str) -> Result<bool, AppError>;

    async fn create(&self, new_bill: &NewBill) -> Result<CreateBillResult, AppError>;
}

#[async_trait]
pub trait IntegrationEventPublisher: Send + Sync {
    async fn publish_bill_created(&self, event: BillCreatedEvent) -> Result<(), AppError>;
}

pub struct CreateBillService {
    bill_write_repository: Arc<dyn BillWriteRepository>,
    integration_event_publisher: Arc<dyn IntegrationEventPublisher>,
}

impl CreateBillService {
    pub fn new(
        bill_write_repository: Arc<dyn BillWriteRepository>,
        integration_event_publisher: Arc<dyn IntegrationEventPublisher>,
    ) -> Self {
        Self {
            bill_write_repository,
            integration_event_publisher,
        }
    }

    pub async fn execute(&self, command: CreateBillCommand) -> Result<CreateBillResult, AppError> {
        if self
            .bill_write_repository
            .exists_by_bill_number(command.bill_number.trim())
            .await?
        {
            return Err(AppError::Conflict(format!(
                "Bill number '{}' already exists.",
                command.bill_number
            )));
        }

        let mut lines = Vec::with_capacity(command.lines.len());
        for (idx, line) in command.lines.iter().enumerate() {
            let new_line = NewBillLine::create(
                (idx + 1) as i32,
                &line.concept,
                line.quantity,
                line.unit_amount,
            )
            .map_err(|e| AppError::Validation(e.errors))?;
            lines.push(new_line);
        }

        let new_bill = NewBill::create(
            &command.bill_number,
            command.issued_at,
            &command.customer_name,
            &command.currency,
            command.tax,
            lines,
        )
        .map_err(|e| AppError::Validation(e.errors))?;

        let created = match self.bill_write_repository.create(&new_bill).await {
            Ok(created) => created,
            Err(AppError::Infrastructure(msg))
                if msg.contains("duplicate key value") || msg.contains("unique") =>
            {
                return Err(AppError::Conflict(format!(
                    "Bill number '{}' already exists.",
                    command.bill_number
                )))
            }
            Err(err) => return Err(err),
        };

        self.integration_event_publisher
            .publish_bill_created(BillCreatedEvent {
                bill_id: created.id,
                bill_number: created.bill_number.clone(),
                issued_at: created.issued_at,
                subtotal: created.subtotal,
                tax: created.tax,
                total: created.total,
                currency: created.currency.clone(),
                occurred_at_utc: Utc::now(),
                source: "rust-api".to_string(),
            })
            .await?;

        Ok(created)
    }
}
