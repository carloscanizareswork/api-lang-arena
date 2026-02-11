from datetime import date
from decimal import Decimal
import os
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import FastAPI
from pydantic import BaseModel, field_serializer
from psycopg_pool import ConnectionPool

from app.application.bills.use_cases.list_bills import ListBillsUseCase
from app.infrastructure.persistence.db import create_session
from app.infrastructure.persistence.repositories import SqlAlchemyBillRepository
from app.presentation.schemas import BillResponse as DddBillResponse


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


@asynccontextmanager
async def lifespan(_: FastAPI):
    global minimal_pool
    min_size = int(os.getenv("POSTGRES_POOL_MIN_SIZE", "1"))
    max_size = int(os.getenv("POSTGRES_POOL_MAX_SIZE", "10"))
    minimal_pool = ConnectionPool(conninfo=_conninfo(), min_size=min_size, max_size=max_size, open=False)
    minimal_pool.open(wait=True)
    try:
        yield
    finally:
        minimal_pool.close()


app = FastAPI(title="python-bills-api", lifespan=lifespan)


@app.get("/")
def root() -> dict[str, str]:
    return {"service": "python-bills-api", "status": "ok"}


@app.get("/bills-minimal", response_model=list[MinimalBillResponse])
def get_bills_minimal() -> list[MinimalBillResponse]:
    sql = """
        SELECT id, bill_number, issued_at, total, currency
        FROM bill
        ORDER BY id;
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
