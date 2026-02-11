# api-lang-arena

Language-by-language API playground.

## Structure
- `dot-net/`: .NET implementation
- `python/`: Python implementation
- `docker-compose.yml`: local PostgreSQL service

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
