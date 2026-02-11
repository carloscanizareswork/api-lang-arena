# dot-net

This folder hosts the .NET API implementation.

## Current status
- Minimal API project: `dot-net/BillsApi`
- Runtime target: `.NET 10`
- Endpoints:
  - `GET /bills-minimal` (raw SQL, previous style)
  - `GET /bills` (Vertical Slice + MediatR + EF + DDD building blocks)

## Run locally
```bash
cd dot-net/BillsApi
dotnet run
```

Then call:
```bash
curl http://localhost:5080/bills-minimal
curl http://localhost:5080/bills
```

## Run with Docker Compose
From repository root:
```bash
docker compose up -d --build dotnet-api
curl http://localhost:5080/bills
```
