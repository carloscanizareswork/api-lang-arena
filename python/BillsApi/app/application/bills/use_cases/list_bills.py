from app.application.bills.dtos import BillDto
from app.domain.bills.repositories import BillRepository


class ListBillsUseCase:
    def __init__(self, bill_repository: BillRepository) -> None:
        self._bill_repository = bill_repository

    def execute(self) -> list[BillDto]:
        bills = self._bill_repository.list()
        return [
            BillDto(
                id=b.id,
                bill_number=b.bill_number,
                issued_at=b.issued_at,
                total=float(b.total.amount),
                currency=b.total.currency,
            )
            for b in bills
        ]
