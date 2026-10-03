from __future__ import annotations

from decimal import Decimal

import pytest
from pydantic import ValidationError

from gateway.models import QuoteIn, QuoteOut, ShipmentIn, ShipmentOut
from tests.conftest import QUOTE, SHIPMENT


def test_shipment_out_declares_the_public_fields():
    assert list(ShipmentOut.model_fields) == [
        "id", "reference", "origin", "destination", "carrier_code", "status", "eta",
    ]


def test_shipment_out_ignores_internal_upstream_fields():
    shipment = ShipmentOut.model_validate(SHIPMENT)

    assert "carrier_id" not in shipment.model_dump()


def test_shipment_out_accepts_string_ids_and_missing_eta():
    s = ShipmentOut.model_validate({**SHIPMENT, "id": "abc-1", "eta": None})

    assert s.id == "abc-1" and s.eta is None


def test_shipment_in_uppercases_carrier():
    assert ShipmentIn(origin="NLRTM", destination="DEHAM", carrier_code="blx").carrier_code == "BLX"


@pytest.mark.parametrize("code", ["NS", "NSLX", "N5L", ""])
def test_shipment_in_rejects_bad_carrier_codes(code):
    with pytest.raises(ValidationError):
        ShipmentIn(origin="NLRTM", destination="DEHAM", carrier_code=code)


def test_quote_in_blank_carrier_means_none():
    q = QuoteIn(origin="NLRTM", destination="DEHAM", weight_kg=3, carrier_code="")

    assert q.carrier_code is None


def test_quote_out_uses_decimal_internally_and_numbers_on_the_wire():
    quote = QuoteOut.model_validate(QUOTE)

    assert quote.amount == Decimal("128.4")
    assert quote.breakdown[0].amount == Decimal("118.34")
    assert quote.model_dump(mode="json") == QUOTE


def test_quote_out_breakdown_defaults_to_empty():
    q = QuoteOut.model_validate({"quote_id": "q", "amount": 1, "currency": "EUR"})

    assert q.breakdown == []
