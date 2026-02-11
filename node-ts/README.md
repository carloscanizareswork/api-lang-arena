# Node TypeScript API

Node + TypeScript implementation of the Bills API.

## Endpoints
- `GET /bills-minimal`: raw SQL via pooled `pg` client
- `GET /bills`: DDD read use case via TypeORM repository
- `POST /bills`: DDD create use case with validation, conflict handling, and RabbitMQ `bill.created` event

Default local URL: `http://localhost:5084`
