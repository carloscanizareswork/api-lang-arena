from sqlalchemy import select
from sqlalchemy.orm import Session

from app.domain.bills.entities import Bill, Money
from app.domain.bills.repositories import BillRepository
from app.infrastructure.persistence.models import BillModel


class SqlAlchemyBillRepository(BillRepository):
    def __init__(self, session: Session) -> None:
        self._session = session

    def list(self) -> list[Bill]:
        rows = self._session.execute(select(BillModel).order_by(BillModel.id)).scalars().all()
        return [
            Bill(
                id=row.id,
                bill_number=row.bill_number,
                issued_at=row.issued_at,
                total=Money(amount=row.total, currency=row.currency),
            )
            for row in rows
        ]
