use crate::app_error::{validation_errors_to_map, AppError};
use crate::application::bills::create::CreateBillService;
use crate::application::bills::get::GetBillsService;
use crate::presentation::dto::{BillResponse, CreateBillRequest, CreateBillResponse};
use axum::extract::rejection::JsonRejection;
use axum::extract::State;
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use chrono::NaiveDate;
use rust_decimal::Decimal;
use serde::Serialize;
use sqlx::{FromRow, PgPool};
use std::sync::Arc;
use validator::Validate;

#[derive(Clone)]
pub struct AppState {
    pub minimal_pool: PgPool,
    pub get_bills_service: Arc<GetBillsService>,
    pub create_bill_service: Arc<CreateBillService>,
}

pub fn router(state: AppState) -> Router {
    Router::new()
        .route("/", get(root))
        .route("/bills-minimal", get(get_bills_minimal))
        .route("/bills", get(get_bills).post(create_bill))
        .with_state(state)
}

#[derive(Debug, Serialize)]
struct RootResponse {
    service: String,
    status: String,
}

async fn root() -> Json<RootResponse> {
    Json(RootResponse {
        service: "rust-bills-api".to_string(),
        status: "ok".to_string(),
    })
}

#[derive(Debug, FromRow)]
struct MinimalBillRow {
    id: i64,
    bill_number: String,
    issued_at: NaiveDate,
    total: Decimal,
    currency: String,
}

async fn get_bills_minimal(
    State(state): State<AppState>,
) -> Result<Json<Vec<BillResponse>>, AppError> {
    let sql = r#"
        SELECT b.id,
               b.bill_number,
               b.issued_at,
               COALESCE(SUM(bl.line_amount), 0) + b.tax AS total,
               b.currency::text AS currency
        FROM bill b
        LEFT JOIN bill_line bl ON bl.bill_id = b.id
        GROUP BY b.id, b.bill_number, b.issued_at, b.tax, b.currency
        ORDER BY b.id;
    "#;

    let rows: Vec<MinimalBillRow> = sqlx::query_as(sql)
        .fetch_all(&state.minimal_pool)
        .await
        .map_err(|e| AppError::Infrastructure(e.to_string()))?;

    let bills = rows
        .into_iter()
        .map(|row| BillResponse {
            id: row.id,
            bill_number: row.bill_number,
            issued_at: row.issued_at,
            total: row.total,
            currency: row.currency.trim().to_string(),
        })
        .collect();

    Ok(Json(bills))
}

async fn get_bills(State(state): State<AppState>) -> Result<Json<Vec<BillResponse>>, AppError> {
    let rows = state.get_bills_service.execute().await?;
    let response = rows.into_iter().map(BillResponse::from).collect();
    Ok(Json(response))
}

async fn create_bill(
    State(state): State<AppState>,
    payload: Result<Json<CreateBillRequest>, JsonRejection>,
) -> Result<(StatusCode, Json<CreateBillResponse>), AppError> {
    let Json(request) = payload.map_err(|_| AppError::validation_single("request", "Invalid JSON payload."))?;

    request
        .validate()
        .map_err(|e| AppError::Validation(validation_errors_to_map(&e)))?;

    let command = request.to_command();
    let created = state.create_bill_service.execute(command).await?;

    Ok((StatusCode::CREATED, Json(CreateBillResponse::from(created))))
}
