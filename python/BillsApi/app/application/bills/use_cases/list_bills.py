from app.application.bills.dtos import BillDto
from app.application.bills.ports.bill_read_repository import BillReadRepository


class ListBillsUseCase:
    def __init__(self, bill_repository: BillReadRepository) -> None:
        self._bill_repository = bill_repository

    def execute(self) -> list[BillDto]:
        return self._bill_repository.list()
