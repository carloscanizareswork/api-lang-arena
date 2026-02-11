# go

This folder hosts the Go API implementation.

## Current status
- API project: `go/BillsApi`
- Endpoints:
  - `GET /bills-minimal` (raw SQL, using `database/sql` pooled connections)
  - `GET /bills` (DDD-style layers: domain, application service, infrastructure repository)

## Run with Docker Compose
From repository root:
```bash
docker compose up -d --build go-api
curl http://localhost:5082/bills-minimal
curl http://localhost:5082/bills
```
