from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.application.bills.dtos import BillDto
from app.application.bills.ports.bill_read_repository import BillReadRepository
from app.infrastructure.persistence.models import BillLineModel, BillModel


_TOTAL_EXPR = (
    func.coalesce(func.sum(BillLineModel.line_amount), 0) + BillModel.tax
).label("total")
_LIST_BILLS_STMT = (
    select(
        BillModel.id,
        BillModel.bill_number,
        BillModel.issued_at,
        _TOTAL_EXPR,
        BillModel.currency,
    )
    .outerjoin(BillLineModel, BillLineModel.bill_id == BillModel.id)
    .group_by(
        BillModel.id,
        BillModel.bill_number,
        BillModel.issued_at,
        BillModel.tax,
        BillModel.currency,
    )
    .order_by(BillModel.id)
)


class SqlAlchemyBillRepository(BillReadRepository):
    def __init__(self, session: Session) -> None:
        self._session = session

    def list(self) -> list[BillDto]:
        rows = self._session.execute(_LIST_BILLS_STMT).tuples().all()
        return [
            BillDto(
                id=row[0],
                bill_number=row[1],
                issued_at=row[2],
                total=float(row[3]),
                currency=row[4],
            )
            for row in rows
        ]
