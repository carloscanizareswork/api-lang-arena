# java

This folder hosts the Java API implementation.

## Current status
- API project: `java/BillsApi`
- Endpoints:
  - `GET /bills-minimal` (raw SQL with JdbcTemplate)
  - `GET /bills` (DDD-style layers using Spring Data JPA)
  - `POST /bills` (DDD write path + validation + RabbitMQ event)

## Run with Docker Compose
From repository root:
```bash
docker compose up -d --build java-api
curl http://localhost:5085/bills-minimal
curl http://localhost:5085/bills
```
