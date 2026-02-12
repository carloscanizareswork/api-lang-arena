use chrono::NaiveDate;
use rust_decimal::prelude::FromPrimitive;
use rust_decimal::{Decimal, RoundingStrategy};
use std::collections::HashMap;

#[derive(Debug, Clone)]
pub struct DomainValidationError {
    pub errors: HashMap<String, Vec<String>>,
}

#[derive(Debug, Clone)]
pub struct NewBillLine {
    pub line_no: i32,
    pub concept: String,
    pub quantity: Decimal,
    pub unit_amount: Decimal,
    pub line_amount: Decimal,
}

#[derive(Debug, Clone)]
pub struct NewBill {
    pub bill_number: String,
    pub issued_at: NaiveDate,
    pub customer_name: String,
    pub currency: String,
    pub tax: Decimal,
    pub lines: Vec<NewBillLine>,
    pub subtotal: Decimal,
    pub total: Decimal,
}

impl NewBillLine {
    pub fn create(
        line_no: i32,
        concept: &str,
        quantity: Decimal,
        unit_amount: Decimal,
    ) -> Result<Self, DomainValidationError> {
        let mut errors = HashMap::<String, Vec<String>>::new();

        if line_no <= 0 {
            add_error(&mut errors, "lines.lineNo", "Line number must be greater than zero.");
        }

        if concept.trim().is_empty() {
            add_error(&mut errors, "lines.concept", "Line concept is required.");
        } else if concept.chars().count() > 200 {
            add_error(&mut errors, "lines.concept", "Line concept max length is 200.");
        }

        if quantity <= Decimal::ZERO {
            add_error(
                &mut errors,
                "lines.quantity",
                "Line quantity must be greater than zero.",
            );
        }

        if unit_amount < Decimal::ZERO {
            add_error(
                &mut errors,
                "lines.unitAmount",
                "Line unit amount cannot be negative.",
            );
        }

        if !errors.is_empty() {
            return Err(DomainValidationError { errors });
        }

        let normalized_quantity = money(quantity);
        let normalized_unit_amount = money(unit_amount);
        let line_amount = money(normalized_quantity * normalized_unit_amount);

        Ok(Self {
            line_no,
            concept: concept.trim().to_string(),
            quantity: normalized_quantity,
            unit_amount: normalized_unit_amount,
            line_amount,
        })
    }
}

impl NewBill {
    pub fn create(
        bill_number: &str,
        issued_at: NaiveDate,
        customer_name: &str,
        currency: &str,
        tax: Decimal,
        lines: Vec<NewBillLine>,
    ) -> Result<Self, DomainValidationError> {
        let mut errors = HashMap::<String, Vec<String>>::new();

        let normalized_bill_number = bill_number.trim();
        let normalized_customer_name = customer_name.trim();
        let normalized_currency = currency.trim().to_uppercase();

        if normalized_bill_number.is_empty() {
            add_error(&mut errors, "billNumber", "Bill number is required.");
        } else if normalized_bill_number.chars().count() > 50 {
            add_error(&mut errors, "billNumber", "Bill number max length is 50.");
        }

        if normalized_customer_name.is_empty() {
            add_error(&mut errors, "customerName", "Customer name is required.");
        } else if normalized_customer_name.chars().count() > 200 {
            add_error(
                &mut errors,
                "customerName",
                "Customer name max length is 200.",
            );
        }

        if normalized_currency.chars().count() != 3 {
            add_error(
                &mut errors,
                "currency",
                "Currency must be a 3-letter ISO code.",
            );
        }

        if tax < Decimal::ZERO {
            add_error(&mut errors, "tax", "Tax cannot be negative.");
        }

        if lines.is_empty() {
            add_error(&mut errors, "lines", "At least one line is required.");
        }

        if !errors.is_empty() {
            return Err(DomainValidationError { errors });
        }

        let subtotal = money(lines.iter().fold(Decimal::ZERO, |acc, line| acc + line.line_amount));
        let normalized_tax = money(tax);
        let total = money(subtotal + normalized_tax);

        Ok(Self {
            bill_number: normalized_bill_number.to_string(),
            issued_at,
            customer_name: normalized_customer_name.to_string(),
            currency: normalized_currency,
            tax: normalized_tax,
            lines,
            subtotal,
            total,
        })
    }
}

fn add_error(errors: &mut HashMap<String, Vec<String>>, field: &str, message: &str) {
    errors
        .entry(field.to_string())
        .or_default()
        .push(message.to_string());
}

fn money(value: Decimal) -> Decimal {
    value.round_dp_with_strategy(2, RoundingStrategy::MidpointAwayFromZero)
}

#[allow(dead_code)]
fn decimal(value: i64) -> Decimal {
    Decimal::from_i64(value).unwrap_or(Decimal::ZERO)
}
