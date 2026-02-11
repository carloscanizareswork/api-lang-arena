from datetime import date

from pydantic import BaseModel


class BillResponse(BaseModel):
    id: int
    billNumber: str
    issuedAt: date
    total: float
    currency: str
