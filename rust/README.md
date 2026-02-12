# rust

This folder hosts the Rust API implementation.

## Current status
- API project: `rust/BillsApi`
- Endpoints:
  - `GET /bills-minimal` (raw SQL with `sqlx` pool)
  - `GET /bills` (DDD-style layers using `SeaORM`)
  - `POST /bills` (DDD write path + validation + RabbitMQ event)

## Run with Docker Compose
From repository root:
```bash
docker compose up -d --build rust-api
curl http://localhost:5086/bills-minimal
curl http://localhost:5086/bills
```
