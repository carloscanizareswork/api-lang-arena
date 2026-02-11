from abc import ABC, abstractmethod

from app.domain.bills.entities import Bill


class BillRepository(ABC):
    @abstractmethod
    def list(self) -> list[Bill]:
        raise NotImplementedError
