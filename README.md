# api-lang-arena

Language-by-language API playground.

## Structure
- `dot-net/`: .NET implementation
- `python/`: Python implementation
- `benchmark-client/`: Python API benchmark client
- `docker-compose.yml`: local PostgreSQL service

## Run .NET API (Docker)
```bash
docker compose up -d --build dotnet-api
curl http://localhost:5080/bills
```

## Run Python API (Docker)
```bash
docker compose up -d --build python-api
curl http://localhost:5081/bills
```

## Run PostgreSQL
1. Copy env file:
   ```bash
   cp .env.example .env
   ```
2. Start database:
   ```bash
   docker compose up -d postgres
   ```
3. Stop database:
   ```bash
   docker compose down
   ```

## Seed Database
1. Ensure Postgres is running on port `5440` (default from `.env.example`).
2. Run the seed script:
   ```bash
   ./db/seed.sh
   ```

This creates `bill` and `bill_line`, then inserts:
- 100 bills
- 10-15 bill lines per bill

## API Benchmark Client
Use the Python benchmark client to compare endpoints across implementations.

```bash
cd benchmark-client
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
python benchmark.py --url http://localhost:8000/health --requests 200 --concurrency 20
```
