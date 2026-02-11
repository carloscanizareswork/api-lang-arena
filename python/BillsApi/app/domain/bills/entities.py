from dataclasses import dataclass, field
from datetime import date
from decimal import Decimal, ROUND_HALF_UP

from app.domain.common.exceptions import DomainValidationError


_MONEY_SCALE = Decimal("0.01")


def _round_money(value: Decimal) -> Decimal:
    return value.quantize(_MONEY_SCALE, rounding=ROUND_HALF_UP)


@dataclass(frozen=True)
class Money:
    amount: Decimal
    currency: str


@dataclass(frozen=True)
class Bill:
    id: int
    bill_number: str
    issued_at: date
    total: Money


@dataclass(frozen=True)
class BillLineDraft:
    concept: str
    quantity: Decimal
    unit_amount: Decimal
    line_amount: Decimal

    @classmethod
    def create(cls, concept: str, quantity: Decimal, unit_amount: Decimal) -> "BillLineDraft":
        if not concept.strip():
            raise DomainValidationError("Line concept is required.")
        if quantity <= 0:
            raise DomainValidationError("Line quantity must be greater than zero.")
        if unit_amount < 0:
            raise DomainValidationError("Line unit amount cannot be negative.")
        line_amount = _round_money(quantity * unit_amount)
        return cls(
            concept=concept.strip(),
            quantity=_round_money(quantity),
            unit_amount=_round_money(unit_amount),
            line_amount=line_amount,
        )


@dataclass
class NewBill:
    bill_number: str
    issued_at: date
    customer_name: str
    currency: str
    tax: Decimal
    lines: list[BillLineDraft] = field(default_factory=list)

    @classmethod
    def create(
        cls,
        bill_number: str,
        issued_at: date,
        customer_name: str,
        currency: str,
        tax: Decimal,
        lines: list[BillLineDraft],
    ) -> "NewBill":
        normalized_bill_number = bill_number.strip()
        normalized_customer_name = customer_name.strip()
        normalized_currency = currency.strip().upper()

        if not normalized_bill_number:
            raise DomainValidationError("Bill number is required.")
        if len(normalized_bill_number) > 50:
            raise DomainValidationError("Bill number max length is 50.")
        if not normalized_customer_name:
            raise DomainValidationError("Customer name is required.")
        if len(normalized_customer_name) > 200:
            raise DomainValidationError("Customer name max length is 200.")
        if len(normalized_currency) != 3:
            raise DomainValidationError("Currency must be a 3-letter ISO code.")
        if issued_at == date.min:
            raise DomainValidationError("Issued date is required.")
        if tax < 0:
            raise DomainValidationError("Tax cannot be negative.")
        if len(lines) == 0:
            raise DomainValidationError("At least one line is required.")

        return cls(
            bill_number=normalized_bill_number,
            issued_at=issued_at,
            customer_name=normalized_customer_name,
            currency=normalized_currency,
            tax=_round_money(tax),
            lines=lines,
        )

    @property
    def subtotal(self) -> Decimal:
        return _round_money(sum((line.line_amount for line in self.lines), Decimal("0")))

    @property
    def total(self) -> Decimal:
        return _round_money(self.subtotal + self.tax)
