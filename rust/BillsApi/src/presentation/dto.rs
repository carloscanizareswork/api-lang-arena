use crate::application::bills::create::{CreateBillCommand, CreateBillLineCommand, CreateBillResult};
use crate::application::bills::get::BillSummaryDto;
use chrono::NaiveDate;
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};
use validator::{Validate, ValidationError};

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct BillResponse {
    pub id: i64,
    pub bill_number: String,
    pub issued_at: NaiveDate,
    pub total: Decimal,
    pub currency: String,
}

impl From<BillSummaryDto> for BillResponse {
    fn from(value: BillSummaryDto) -> Self {
        Self {
            id: value.id,
            bill_number: value.bill_number,
            issued_at: value.issued_at,
            total: value.total,
            currency: value.currency,
        }
    }
}

#[derive(Debug, Clone, Deserialize, Serialize, Validate)]
#[serde(rename_all = "camelCase")]
pub struct CreateBillLineRequest {
    #[validate(length(min = 1, max = 200, message = "Line concept is required."))]
    pub concept: String,

    #[validate(custom(function = "validate_positive_decimal"))]
    pub quantity: Decimal,

    #[validate(custom(function = "validate_non_negative_line_unit_amount"))]
    pub unit_amount: Decimal,
}

#[derive(Debug, Clone, Deserialize, Serialize, Validate)]
#[serde(rename_all = "camelCase")]
pub struct CreateBillRequest {
    #[validate(length(min = 1, max = 50, message = "Bill number is required."))]
    pub bill_number: String,

    pub issued_at: NaiveDate,

    #[validate(length(min = 1, max = 200, message = "Customer name is required."))]
    pub customer_name: String,

    #[validate(length(equal = 3, message = "Currency must be a 3-letter ISO code."))]
    pub currency: String,

    #[validate(custom(function = "validate_non_negative_tax"))]
    pub tax: Decimal,

    #[validate(length(min = 1, message = "At least one line is required."), nested)]
    pub lines: Vec<CreateBillLineRequest>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CreateBillResponse {
    pub id: i64,
    pub bill_number: String,
    pub issued_at: NaiveDate,
    pub subtotal: Decimal,
    pub tax: Decimal,
    pub total: Decimal,
    pub currency: String,
}

impl From<CreateBillResult> for CreateBillResponse {
    fn from(value: CreateBillResult) -> Self {
        Self {
            id: value.id,
            bill_number: value.bill_number,
            issued_at: value.issued_at,
            subtotal: value.subtotal,
            tax: value.tax,
            total: value.total,
            currency: value.currency,
        }
    }
}

impl CreateBillRequest {
    pub fn to_command(&self) -> CreateBillCommand {
        CreateBillCommand {
            bill_number: self.bill_number.clone(),
            issued_at: self.issued_at,
            customer_name: self.customer_name.clone(),
            currency: self.currency.clone(),
            tax: self.tax,
            lines: self
                .lines
                .iter()
                .map(|line| CreateBillLineCommand {
                    concept: line.concept.clone(),
                    quantity: line.quantity,
                    unit_amount: line.unit_amount,
                })
                .collect(),
        }
    }
}

fn validate_positive_decimal(value: &Decimal) -> Result<(), ValidationError> {
    if *value > Decimal::ZERO {
        return Ok(());
    }

    let mut error = ValidationError::new("positive_decimal");
    error.message = Some("Line quantity must be greater than zero.".into());
    Err(error)
}

fn validate_non_negative_line_unit_amount(value: &Decimal) -> Result<(), ValidationError> {
    if *value >= Decimal::ZERO {
        return Ok(());
    }

    let mut error = ValidationError::new("non_negative_decimal");
    error.message = Some("Line unit amount cannot be negative.".into());
    Err(error)
}

fn validate_non_negative_tax(value: &Decimal) -> Result<(), ValidationError> {
    if *value >= Decimal::ZERO {
        return Ok(());
    }

    let mut error = ValidationError::new("non_negative_decimal");
    error.message = Some("Tax cannot be negative.".into());
    Err(error)
}
