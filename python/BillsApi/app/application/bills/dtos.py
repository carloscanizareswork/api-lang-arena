from dataclasses import dataclass
from datetime import date


@dataclass(frozen=True)
class BillDto:
    id: int
    bill_number: str
    issued_at: date
    total: float
    currency: str
