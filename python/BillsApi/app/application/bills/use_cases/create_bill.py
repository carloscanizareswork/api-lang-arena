from dataclasses import dataclass
from datetime import date, datetime, timezone
from decimal import Decimal

from sqlalchemy.exc import IntegrityError

from app.application.bills.ports.bill_write_repository import BillWriteRepository
from app.application.bills.ports.integration_event_publisher import IntegrationEventPublisher
from app.application.common.exceptions import ConflictError
from app.domain.bills.entities import BillLineDraft, NewBill


@dataclass(frozen=True)
class CreateBillLineCommand:
    concept: str
    quantity: Decimal
    unit_amount: Decimal


@dataclass(frozen=True)
class CreateBillCommand:
    bill_number: str
    issued_at: date
    customer_name: str
    currency: str
    tax: Decimal
    lines: list[CreateBillLineCommand]


@dataclass(frozen=True)
class CreateBillResult:
    id: int
    bill_number: str
    issued_at: date
    subtotal: float
    tax: float
    total: float
    currency: str


class CreateBillUseCase:
    def __init__(
        self,
        bill_write_repository: BillWriteRepository,
        integration_event_publisher: IntegrationEventPublisher,
    ) -> None:
        self._bill_write_repository = bill_write_repository
        self._integration_event_publisher = integration_event_publisher

    def execute(self, command: CreateBillCommand) -> CreateBillResult:
        if self._bill_write_repository.exists_by_bill_number(command.bill_number.strip()):
            raise ConflictError(f"Bill number '{command.bill_number}' already exists.")

        lines = [
            BillLineDraft.create(line.concept, line.quantity, line.unit_amount)
            for line in command.lines
        ]
        new_bill = NewBill.create(
            bill_number=command.bill_number,
            issued_at=command.issued_at,
            customer_name=command.customer_name,
            currency=command.currency,
            tax=command.tax,
            lines=lines,
        )

        try:
            created_bill_id = self._bill_write_repository.create(new_bill)
        except IntegrityError as exc:
            raise ConflictError(f"Bill number '{command.bill_number}' already exists.") from exc

        event_payload = {
            "billId": created_bill_id,
            "billNumber": new_bill.bill_number,
            "issuedAt": new_bill.issued_at.isoformat(),
            "subtotal": float(new_bill.subtotal),
            "tax": float(new_bill.tax),
            "total": float(new_bill.total),
            "currency": new_bill.currency,
            "occurredAtUtc": datetime.now(timezone.utc).isoformat(),
            "source": "python-api",
        }
        self._integration_event_publisher.publish("bill.created", event_payload)

        return CreateBillResult(
            id=created_bill_id,
            bill_number=new_bill.bill_number,
            issued_at=new_bill.issued_at,
            subtotal=float(new_bill.subtotal),
            tax=float(new_bill.tax),
            total=float(new_bill.total),
            currency=new_bill.currency,
        )
