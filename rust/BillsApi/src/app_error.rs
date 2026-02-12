use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use axum::Json;
use serde::Serialize;
use std::collections::HashMap;
use thiserror::Error;
use tracing::error;
use validator::{ValidationErrors, ValidationErrorsKind};

#[derive(Debug, Error)]
pub enum AppError {
    #[error("validation failed")]
    Validation(HashMap<String, Vec<String>>),

    #[error("conflict: {0}")]
    Conflict(String),

    #[error("message broker error: {0}")]
    Messaging(String),

    #[error("infrastructure error: {0}")]
    Infrastructure(String),
}

#[derive(Debug, Serialize)]
struct ProblemResponse {
    #[serde(rename = "type")]
    problem_type: String,
    title: String,
    status: u16,
    errors: Option<HashMap<String, Vec<String>>>,
}

impl AppError {
    pub fn validation_single(field: &str, message: &str) -> Self {
        let mut errors = HashMap::new();
        errors.insert(field.to_string(), vec![message.to_string()]);
        Self::Validation(errors)
    }
}

impl IntoResponse for AppError {
    fn into_response(self) -> Response {
        match self {
            AppError::Validation(errors) => problem(StatusCode::BAD_REQUEST, "Validation failed", Some(errors)),
            AppError::Conflict(message) => problem(StatusCode::CONFLICT, &message, None),
            AppError::Messaging(message) => {
                error!(error = %message, "RabbitMQ publish failed");
                problem(
                    StatusCode::SERVICE_UNAVAILABLE,
                    &format!("Message broker error: {message}"),
                    None,
                )
            }
            AppError::Infrastructure(message) => {
                error!(error = %message, "Unhandled infrastructure error");
                problem(
                    StatusCode::INTERNAL_SERVER_ERROR,
                    "An unexpected error occurred.",
                    None,
                )
            }
        }
    }
}

pub fn validation_errors_to_map(errors: &ValidationErrors) -> HashMap<String, Vec<String>> {
    let mut mapped = HashMap::new();
    collect_validation_errors("", errors, &mut mapped);
    mapped
}

fn collect_validation_errors(
    prefix: &str,
    errors: &ValidationErrors,
    out: &mut HashMap<String, Vec<String>>,
) {
    for (field, kind) in errors.errors() {
        match kind {
            ValidationErrorsKind::Field(field_errors) => {
                let normalized_field = snake_to_camel(field);
                let key = if prefix.is_empty() {
                    normalized_field
                } else {
                    format!("{prefix}{normalized_field}")
                };
                for err in field_errors {
                    let message = err
                        .message
                        .as_ref()
                        .map(ToString::to_string)
                        .unwrap_or_else(|| "Invalid value".to_string());
                    out.entry(key.clone()).or_default().push(message);
                }
            }
            ValidationErrorsKind::Struct(inner) => {
                let normalized_field = snake_to_camel(field);
                let nested_prefix = if prefix.is_empty() {
                    format!("{normalized_field}.")
                } else {
                    format!("{prefix}{normalized_field}.")
                };
                collect_validation_errors(&nested_prefix, inner, out);
            }
            ValidationErrorsKind::List(list) => {
                let normalized_field = snake_to_camel(field);
                let nested_prefix = if prefix.is_empty() {
                    format!("{normalized_field}.")
                } else {
                    format!("{prefix}{normalized_field}.")
                };
                for inner in list.values() {
                    collect_validation_errors(&nested_prefix, inner, out);
                }
            }
        }
    }
}

fn snake_to_camel(value: &str) -> String {
    let mut output = String::new();
    let mut uppercase_next = false;

    for ch in value.chars() {
        if ch == '_' {
            uppercase_next = true;
            continue;
        }
        if uppercase_next {
            output.push(ch.to_ascii_uppercase());
            uppercase_next = false;
        } else {
            output.push(ch);
        }
    }

    output
}

fn problem(status: StatusCode, title: &str, errors: Option<HashMap<String, Vec<String>>>) -> Response {
    let body = ProblemResponse {
        problem_type: "about:blank".to_string(),
        title: title.to_string(),
        status: status.as_u16(),
        errors,
    };

    (status, Json(body)).into_response()
}
