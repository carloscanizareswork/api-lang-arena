# api-lang-arena

Language-by-language API playground.

## Structure
- `dot-net/`: .NET API implementation (`minimal` + `DDD`)
- `python/`: Python API implementation (`minimal` + `DDD`)
- `go/`: Go API implementation (`minimal` + `DDD`)
- `kotlin/`: Kotlin API implementation (`minimal` + `DDD`)
- `node-ts/`: Node + TypeScript API implementation (`minimal` + `DDD`)
- `java/`: Java API implementation (`minimal` + `DDD`)
- `benchmark-client/`: Python API benchmark client
- `docker-compose.yml`: local PostgreSQL + RabbitMQ + APIs

## API Endpoints
- .NET minimal: `GET http://localhost:5080/bills-minimal`
- .NET DDD: `GET http://localhost:5080/bills`
- .NET DDD POST: `POST http://localhost:5080/bills`
- Python minimal: `GET http://localhost:5081/bills-minimal`
- Python DDD: `GET http://localhost:5081/bills`
- Python DDD POST: `POST http://localhost:5081/bills`
- Go minimal: `GET http://localhost:5082/bills-minimal`
- Go DDD: `GET http://localhost:5082/bills`
- Go DDD POST: `POST http://localhost:5082/bills`
- Kotlin minimal: `GET http://localhost:5083/bills-minimal`
- Kotlin DDD: `GET http://localhost:5083/bills`
- Kotlin DDD POST: `POST http://localhost:5083/bills`
- Node minimal: `GET http://localhost:5084/bills-minimal`
- Node DDD: `GET http://localhost:5084/bills`
- Node DDD POST: `POST http://localhost:5084/bills`
- Java minimal: `GET http://localhost:5085/bills-minimal`
- Java DDD: `GET http://localhost:5085/bills`
- Java DDD POST: `POST http://localhost:5085/bills`

## Start Services (Docker)
```bash
cp .env.example .env
docker compose up -d --build postgres rabbitmq dotnet-api python-api go-api kotlin-api node-api java-api
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
curl http://localhost:5083/bills-minimal
curl http://localhost:5083/bills
curl http://localhost:5084/bills-minimal
curl http://localhost:5084/bills
curl http://localhost:5085/bills-minimal
curl http://localhost:5085/bills
```

## Benchmark Comparison
`run_compare.sh` benchmarks all GET endpoints using the same load profile:
- `.NET-Min`
- `.NET-DDD`
- `Py-Min`
- `Py-DDD`
- `Go-Min`
- `Go-DDD`
- `Kt-Min`
- `Kt-DDD`
- `Node-Min`
- `Node-DDD`
- `Java-Min`
- `Java-DDD`

It also benchmarks POST `/bills` for all configured languages:
- `.NET-Post`
- `Py-Post`
- `Go-Post`
- `Kt-Post`
- `Node-Post`
- `Java-Post`

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
