from datetime import date

from pydantic import BaseModel, Field, model_validator


class BillResponse(BaseModel):
    id: int
    billNumber: str
    issuedAt: date
    total: float
    currency: str


class CreateBillLineRequest(BaseModel):
    concept: str = Field(min_length=1, max_length=200)
    quantity: float = Field(gt=0)
    unitAmount: float = Field(ge=0)


class CreateBillRequest(BaseModel):
    billNumber: str = Field(min_length=1, max_length=50)
    issuedAt: date
    customerName: str = Field(min_length=1, max_length=200)
    currency: str = Field(min_length=3, max_length=3)
    tax: float = Field(ge=0)
    lines: list[CreateBillLineRequest] = Field(min_length=1)

    @model_validator(mode="after")
    def normalize_currency(self) -> "CreateBillRequest":
        self.currency = self.currency.strip().upper()
        return self


class CreateBillResponse(BaseModel):
    id: int
    billNumber: str
    issuedAt: date
    subtotal: float
    tax: float
    total: float
    currency: str
