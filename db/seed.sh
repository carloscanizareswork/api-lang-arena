#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEED_SQL="${SCRIPT_DIR}/seed.sql"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5440}"
DB_NAME="${DB_NAME:-api_lang_arena}"
DB_USER="${DB_USER:-api_lang_user}"
DB_PASSWORD="${DB_PASSWORD:-api_lang_password}"
DB_CONTAINER="${DB_CONTAINER:-api-lang-arena-postgres}"

export PGPASSWORD="${DB_PASSWORD}"

echo "Seeding database ${DB_NAME} on ${DB_HOST}:${DB_PORT} as ${DB_USER}..."
if command -v psql >/dev/null 2>&1; then
  psql \
    --host "${DB_HOST}" \
    --port "${DB_PORT}" \
    --username "${DB_USER}" \
    --dbname "${DB_NAME}" \
    --set ON_ERROR_STOP=1 \
    --file "${SEED_SQL}"
else
  echo "Local psql not found. Using docker container ${DB_CONTAINER}..."
  docker exec -i "${DB_CONTAINER}" env PGPASSWORD="${DB_PASSWORD}" \
    psql \
      --username "${DB_USER}" \
      --dbname "${DB_NAME}" \
      --set ON_ERROR_STOP=1 < "${SEED_SQL}"
fi

echo "Seed complete."
