use crate::app_error::AppError;
use async_trait::async_trait;
use chrono::NaiveDate;
use rust_decimal::Decimal;
use std::sync::Arc;

#[derive(Debug, Clone)]
pub struct BillSummaryDto {
    pub id: i64,
    pub bill_number: String,
    pub issued_at: NaiveDate,
    pub total: Decimal,
    pub currency: String,
}

#[async_trait]
pub trait BillReadRepository: Send + Sync {
    async fn list_bills(&self) -> Result<Vec<BillSummaryDto>, AppError>;
}

pub struct GetBillsService {
    bill_read_repository: Arc<dyn BillReadRepository>,
}

impl GetBillsService {
    pub fn new(bill_read_repository: Arc<dyn BillReadRepository>) -> Self {
        Self { bill_read_repository }
    }

    pub async fn execute(&self) -> Result<Vec<BillSummaryDto>, AppError> {
        self.bill_read_repository.list_bills().await
    }
}
