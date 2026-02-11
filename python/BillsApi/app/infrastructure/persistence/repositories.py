from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.application.bills.dtos import BillDto
from app.application.bills.ports.bill_read_repository import BillReadRepository
from app.application.bills.ports.bill_write_repository import BillWriteRepository
from app.domain.bills.entities import NewBill
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


class SqlAlchemyBillRepository(BillReadRepository, BillWriteRepository):
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

    def exists_by_bill_number(self, bill_number: str) -> bool:
        stmt = select(BillModel.id).where(BillModel.bill_number == bill_number).limit(1)
        return self._session.execute(stmt).first() is not None

    def create(self, new_bill: NewBill) -> int:
        try:
            bill_row = BillModel(
                bill_number=new_bill.bill_number,
                issued_at=new_bill.issued_at,
                customer_name=new_bill.customer_name,
                subtotal=new_bill.subtotal,
                tax=new_bill.tax,
                currency=new_bill.currency,
            )

            self._session.add(bill_row)
            self._session.flush()

            line_rows = [
                BillLineModel(
                    bill_id=bill_row.id,
                    line_no=idx,
                    concept=line.concept,
                    quantity=line.quantity,
                    unit_amount=line.unit_amount,
                    line_amount=line.line_amount,
                )
                for idx, line in enumerate(new_bill.lines, start=1)
            ]
            self._session.add_all(line_rows)
            self._session.commit()
            return int(bill_row.id)
        except Exception:
            self._session.rollback()
            raise
