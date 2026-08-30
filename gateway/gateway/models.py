"""Public request/response models of the gateway."""

from __future__ import annotations

from datetime import datetime
from decimal import Decimal
from typing import Annotated, Any

from pydantic import BaseModel, Field, PlainSerializer, field_validator

# Money is parsed as Decimal and written as a JSON number on the wire.
Money = Annotated[
    Decimal,
    PlainSerializer(lambda value: float(value), return_type=float, when_used="json"),
]


def _normalise_carrier_code(value: Any) -> Any:
    if isinstance(value, str):
        return value.strip().upper()
    return value


class ShipmentIn(BaseModel):
    reference: str | None = Field(default=None, min_length=4, max_length=64)
    origin: str = Field(min_length=2, max_length=64)
    destination: str = Field(min_length=2, max_length=64)
    carrier_code: str = Field(pattern=r"^[A-Z]{3}$")
    weight_kg: float | None = Field(default=None, gt=0, le=40000)

    @field_validator("carrier_code", mode="before")
    @classmethod
    def _upper(cls, value: Any) -> Any:
        return _normalise_carrier_code(value)


class ShipmentOut(BaseModel):
    id: int | str
    reference: str
    origin: str
    destination: str
    carrier_code: str
    status: str
    eta: datetime | None = None


class QuoteIn(BaseModel):
    origin: str = Field(min_length=2, max_length=64)
    destination: str = Field(min_length=2, max_length=64)
    weight_kg: float = Field(gt=0, le=40000)
    carrier_code: str | None = Field(default=None, pattern=r"^[A-Z]{3}$")

    @field_validator("carrier_code", mode="before")
    @classmethod
    def _upper(cls, value: Any) -> Any:
        if value in ("", None):
            return None
        return _normalise_carrier_code(value)


class BreakdownLine(BaseModel):
    label: str
    amount: Money


class QuoteOut(BaseModel):
    quote_id: str
    amount: Money
    currency: str
    breakdown: list[BreakdownLine] = Field(default_factory=list)


class CarrierOut(BaseModel):
    code: str
    name: str
    active: bool = True


class SurchargeLine(BaseModel):
    code: str
    label: str | None = None
    pct: Money | None = None
    amount: Money | None = None


class SurchargesOut(BaseModel):
    carrier_code: str
    surcharges: list[SurchargeLine] = Field(default_factory=list)
