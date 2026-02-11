# python

This folder hosts the Python API implementation.

## Current status
- Minimal API project: `python/BillsApi`
- Endpoints:
  - `GET /bills-minimal` (raw SQL style)
  - `GET /bills` (DDD-style layers: domain, application use case, infrastructure repository)
- Both endpoints read from PostgreSQL (`bill` table)

## Run with Docker Compose
From repository root:
```bash
docker compose up -d --build python-api
curl http://localhost:5081/bills-minimal
curl http://localhost:5081/bills
```
