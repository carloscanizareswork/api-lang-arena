from datetime import date
from decimal import Decimal
import os
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
import pika
from pydantic import BaseModel, field_serializer
from psycopg_pool import ConnectionPool

from app.application.bills.use_cases.create_bill import (
    CreateBillCommand,
    CreateBillLineCommand,
    CreateBillUseCase,
)
from app.application.bills.use_cases.list_bills import ListBillsUseCase
from app.application.common.exceptions import ConflictError
from app.domain.common.exceptions import DomainValidationError
from app.infrastructure.messaging.rabbitmq_publisher import RabbitMqIntegrationEventPublisher
from app.infrastructure.persistence.db import create_session
from app.infrastructure.persistence.repositories import SqlAlchemyBillRepository
from app.presentation.schemas import (
    BillResponse as DddBillResponse,
    CreateBillRequest,
    CreateBillResponse,
)


class MinimalBillResponse(BaseModel):
    id: int
    billNumber: str
    issuedAt: date
    total: Decimal
    currency: str

    @field_serializer("total")
    def serialize_total(self, value: Decimal) -> float:
        return float(value)


minimal_pool: Optional[ConnectionPool] = None
event_publisher: Optional[RabbitMqIntegrationEventPublisher] = None


def _conninfo() -> str:
    host = os.getenv("POSTGRES_HOST", "localhost")
    port = os.getenv("POSTGRES_PORT", "5440")
    db = os.getenv("POSTGRES_DB", "api_lang_arena")
    user = os.getenv("POSTGRES_USER", "api_lang_user")
    password = os.getenv("POSTGRES_PASSWORD", "api_lang_password")
    return f"host={host} port={port} dbname={db} user={user} password={password}"


def _get_minimal_pool() -> ConnectionPool:
    if minimal_pool is None:
        raise RuntimeError("Minimal connection pool is not initialized.")
    return minimal_pool


def _get_event_publisher() -> RabbitMqIntegrationEventPublisher:
    if event_publisher is None:
        raise RuntimeError("Event publisher is not initialized.")
    return event_publisher


@asynccontextmanager
async def lifespan(_: FastAPI):
    global minimal_pool, event_publisher
    min_size = int(os.getenv("POSTGRES_POOL_MIN_SIZE", "1"))
    max_size = int(os.getenv("POSTGRES_POOL_MAX_SIZE", "10"))
    minimal_pool = ConnectionPool(conninfo=_conninfo(), min_size=min_size, max_size=max_size, open=False)
    minimal_pool.open(wait=True)
    event_publisher = RabbitMqIntegrationEventPublisher(
        host=os.getenv("RABBITMQ_HOST", "localhost"),
        port=int(os.getenv("RABBITMQ_PORT", "5672")),
        user=os.getenv("RABBITMQ_USER", "guest"),
        password=os.getenv("RABBITMQ_PASSWORD", "guest"),
        vhost=os.getenv("RABBITMQ_VHOST", "/"),
        queue_name=os.getenv("RABBITMQ_BILL_CREATED_QUEUE", "bill-created"),
    )
    try:
        yield
    finally:
        if event_publisher is not None:
            event_publisher.close()
        minimal_pool.close()


app = FastAPI(title="python-bills-api", lifespan=lifespan)


@app.get("/")
def root() -> dict[str, str]:
    return {"service": "python-bills-api", "status": "ok"}


def _problem(status: int, title: str, errors: Optional[dict[str, list[str]]] = None) -> JSONResponse:
    return JSONResponse(
        status_code=status,
        content={
            "type": "about:blank",
            "title": title,
            "status": status,
            "errors": errors,
        },
    )


@app.exception_handler(RequestValidationError)
def request_validation_exception_handler(_: Request, exc: RequestValidationError) -> JSONResponse:
    mapped: dict[str, list[str]] = {}
    for err in exc.errors():
        location = ".".join(str(item) for item in err.get("loc", []))
        field_name = location.split(".")[-1] if location else "request"
        mapped.setdefault(field_name, []).append(err.get("msg", "Invalid value"))
    return _problem(400, "Validation failed", mapped)


@app.exception_handler(DomainValidationError)
def domain_validation_exception_handler(_: Request, exc: DomainValidationError) -> JSONResponse:
    return _problem(400, str(exc))


@app.exception_handler(ConflictError)
def conflict_exception_handler(_: Request, exc: ConflictError) -> JSONResponse:
    return _problem(409, str(exc))


@app.exception_handler(pika.exceptions.AMQPError)
def rabbitmq_exception_handler(_: Request, exc: pika.exceptions.AMQPError) -> JSONResponse:
    return _problem(503, f"Message broker error: {exc.__class__.__name__}")


@app.exception_handler(Exception)
def generic_exception_handler(_: Request, __: Exception) -> JSONResponse:
    return _problem(500, "An unexpected error occurred.")


@app.get("/bills-minimal", response_model=list[MinimalBillResponse])
def get_bills_minimal() -> list[MinimalBillResponse]:
    sql = """
        SELECT b.id,
               b.bill_number,
               b.issued_at,
               COALESCE(SUM(bl.line_amount), 0) + b.tax AS total,
               b.currency
        FROM bill b
        LEFT JOIN bill_line bl ON bl.bill_id = b.id
        GROUP BY b.id, b.bill_number, b.issued_at, b.tax, b.currency
        ORDER BY b.id;
    """
    pool = _get_minimal_pool()
    with pool.connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql)
            rows = cur.fetchall()

    return [
        MinimalBillResponse(
            id=row[0],
            billNumber=row[1],
            issuedAt=row[2],
            total=row[3],
            currency=row[4],
        )
        for row in rows
    ]


@app.get("/bills", response_model=list[DddBillResponse])
def get_bills() -> list[DddBillResponse]:
    with create_session() as session:
        repository = SqlAlchemyBillRepository(session)
        use_case = ListBillsUseCase(repository)
        bills = use_case.execute()

    return [
        DddBillResponse(
            id=b.id,
            billNumber=b.bill_number,
            issuedAt=b.issued_at,
            total=b.total,
            currency=b.currency,
        )
        for b in bills
    ]


@app.post("/bills", response_model=CreateBillResponse, status_code=201)
def create_bill(request: CreateBillRequest) -> CreateBillResponse:
    with create_session() as session:
        repository = SqlAlchemyBillRepository(session)
        use_case = CreateBillUseCase(repository, _get_event_publisher())
        command = CreateBillCommand(
            bill_number=request.billNumber,
            issued_at=request.issuedAt,
            customer_name=request.customerName,
            currency=request.currency,
            tax=Decimal(str(request.tax)),
            lines=[
                CreateBillLineCommand(
                    concept=line.concept,
                    quantity=Decimal(str(line.quantity)),
                    unit_amount=Decimal(str(line.unitAmount)),
                )
                for line in request.lines
            ],
        )
        created = use_case.execute(command)

    return CreateBillResponse(
        id=created.id,
        billNumber=created.bill_number,
        issuedAt=created.issued_at,
        subtotal=created.subtotal,
        tax=created.tax,
        total=created.total,
        currency=created.currency,
    )
