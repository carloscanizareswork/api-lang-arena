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
