from abc import ABC, abstractmethod

from app.domain.bills.entities import NewBill


class BillWriteRepository(ABC):
    @abstractmethod
    def exists_by_bill_number(self, bill_number: str) -> bool:
        raise NotImplementedError

    @abstractmethod
    def create(self, new_bill: NewBill) -> int:
        raise NotImplementedError
