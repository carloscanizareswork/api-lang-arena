use crate::app_error::AppError;
use crate::application::bills::create::{BillWriteRepository, CreateBillResult};
use crate::application::bills::get::{BillReadRepository, BillSummaryDto};
use crate::domain::bills::NewBill;
use crate::infrastructure::persistence::entities::{bill, bill_line};
use async_trait::async_trait;
use rust_decimal::Decimal;
use sea_orm::prelude::Date;
use sea_orm::{
    ActiveModelTrait, ColumnTrait, DatabaseConnection, DbErr, EntityTrait, JoinType, PaginatorTrait,
    QueryFilter, QueryOrder, QuerySelect, RelationTrait, Set, TransactionTrait,
};
use sea_orm::FromQueryResult;

#[derive(Clone)]
pub struct SeaOrmBillRepository {
    db: DatabaseConnection,
}

impl SeaOrmBillRepository {
    pub fn new(db: DatabaseConnection) -> Self {
        Self { db }
    }
}

#[derive(Debug, FromQueryResult)]
struct BillSummaryRow {
    id: i64,
    bill_number: String,
    issued_at: Date,
    total: Decimal,
    currency: String,
}

#[async_trait]
impl BillReadRepository for SeaOrmBillRepository {
    async fn list_bills(&self) -> Result<Vec<BillSummaryDto>, AppError> {
        let rows: Vec<BillSummaryRow> = bill::Entity::find()
            .select_only()
            .column_as(bill::Column::Id, "id")
            .column_as(bill::Column::BillNumber, "bill_number")
            .column_as(bill::Column::IssuedAt, "issued_at")
            .expr_as(
                sea_orm::sea_query::Expr::cust("COALESCE(SUM(bill_line.line_amount), 0) + bill.tax"),
                "total",
            )
            .column_as(bill::Column::Currency, "currency")
            .join(JoinType::LeftJoin, bill::Relation::BillLine.def())
            .group_by(bill::Column::Id)
            .group_by(bill::Column::BillNumber)
            .group_by(bill::Column::IssuedAt)
            .group_by(bill::Column::Tax)
            .group_by(bill::Column::Currency)
            .order_by_asc(bill::Column::Id)
            .into_model::<BillSummaryRow>()
            .all(&self.db)
            .await
            .map_err(|e| AppError::Infrastructure(e.to_string()))?;

        Ok(rows
            .into_iter()
            .map(|row| BillSummaryDto {
                id: row.id,
                bill_number: row.bill_number,
                issued_at: row.issued_at,
                total: row.total,
                currency: row.currency.trim().to_string(),
            })
            .collect())
    }
}

#[async_trait]
impl BillWriteRepository for SeaOrmBillRepository {
    async fn exists_by_bill_number(&self, bill_number: &str) -> Result<bool, AppError> {
        let count = bill::Entity::find()
            .filter(bill::Column::BillNumber.eq(bill_number))
            .count(&self.db)
            .await
            .map_err(|e| AppError::Infrastructure(e.to_string()))?;
        Ok(count > 0)
    }

    async fn create(&self, new_bill: &NewBill) -> Result<CreateBillResult, AppError> {
        let txn = self
            .db
            .begin()
            .await
            .map_err(|e| AppError::Infrastructure(e.to_string()))?;

        let saved_bill = bill::ActiveModel {
            bill_number: Set(new_bill.bill_number.clone()),
            issued_at: Set(new_bill.issued_at),
            customer_name: Set(new_bill.customer_name.clone()),
            subtotal: Set(new_bill.subtotal),
            tax: Set(new_bill.tax),
            currency: Set(new_bill.currency.clone()),
            ..Default::default()
        }
        .insert(&txn)
        .await
        .map_err(map_db_error)?;

        for line in &new_bill.lines {
            bill_line::ActiveModel {
                bill_id: Set(saved_bill.id),
                line_no: Set(line.line_no),
                concept: Set(line.concept.clone()),
                quantity: Set(line.quantity),
                unit_amount: Set(line.unit_amount),
                line_amount: Set(line.line_amount),
                ..Default::default()
            }
            .insert(&txn)
            .await
            .map_err(map_db_error)?;
        }

        txn.commit()
            .await
            .map_err(|e| AppError::Infrastructure(e.to_string()))?;

        Ok(CreateBillResult {
            id: saved_bill.id,
            bill_number: saved_bill.bill_number,
            issued_at: saved_bill.issued_at,
            subtotal: new_bill.subtotal,
            tax: new_bill.tax,
            total: new_bill.total,
            currency: saved_bill.currency.trim().to_string(),
        })
    }
}

fn map_db_error(error: DbErr) -> AppError {
    AppError::Infrastructure(error.to_string())
}
