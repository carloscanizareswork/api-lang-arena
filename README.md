# api-lang-arena

Language-by-language API playground.

## Structure
- `dot-net/`: .NET API implementation (`minimal` + `DDD`)
- `python/`: Python API implementation (`minimal` + `DDD`)
- `go/`: Go API implementation (`minimal` + `DDD`)
- `benchmark-client/`: Python API benchmark client
- `docker-compose.yml`: local PostgreSQL service

## API Endpoints
- .NET minimal: `GET http://localhost:5080/bills-minimal`
- .NET DDD: `GET http://localhost:5080/bills`
- Python minimal: `GET http://localhost:5081/bills-minimal`
- Python DDD: `GET http://localhost:5081/bills`
- Go minimal: `GET http://localhost:5082/bills-minimal`
- Go DDD: `GET http://localhost:5082/bills`

## Start Services (Docker)
```bash
cp .env.example .env
docker compose up -d --build postgres dotnet-api python-api go-api
```

## Seed Database
```bash
./db/seed.sh
```

The seed creates `bill` and `bill_line`, then inserts:
- 100 bills
- 10-15 bill lines per bill

## Quick Smoke Test
```bash
curl http://localhost:5080/bills-minimal
curl http://localhost:5080/bills
curl http://localhost:5081/bills-minimal
curl http://localhost:5081/bills
curl http://localhost:5082/bills-minimal
curl http://localhost:5082/bills
```

## Benchmark Comparison
`run_compare.sh` benchmarks all four endpoints using the same load profile:
- `.NET-Min`
- `.NET-DDD`
- `Py-Min`
- `Py-DDD`
- `Go-Min`
- `Go-DDD`

```bash
cd benchmark-client
./run_compare.sh
```

Optional benchmark tuning:
```bash
REQUESTS=1000 CONCURRENCY=50 WARMUP_REQUESTS=50 ./run_compare.sh
```

## Stop Services
```bash
docker compose down
```
