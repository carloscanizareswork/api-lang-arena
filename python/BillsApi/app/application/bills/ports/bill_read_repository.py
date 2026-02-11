from abc import ABC, abstractmethod

from app.application.bills.dtos import BillDto


class BillReadRepository(ABC):
    @abstractmethod
    def list(self) -> list[BillDto]:
        raise NotImplementedError
