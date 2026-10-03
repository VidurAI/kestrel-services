from __future__ import annotations

from collections.abc import Iterator

import httpx
import pytest
import respx
from fastapi.testclient import TestClient

from gateway.config import Settings
from gateway.main import create_app

BOOKING = "http://booking.test"
PRICING = "http://pricing.test"

SHIPMENT = {
    "id": 41,
    "reference": "KST-2026-0041",
    "origin": "NLRTM",
    "destination": "DEHAM",
    "carrier_code": "NSL",
    "status": "CREATED",
    "eta": "2026-11-02T10:00:00Z",
    "carrier_id": 1,
}

QUOTE = {
    "quote_id": "q-1001",
    "amount": 128.4,
    "currency": "EUR",
    "breakdown": [
        {"label": "freight", "amount": 118.34},
        {"label": "fuel", "amount": 10.06},
    ],
}

CARRIERS = [
    {"id": 1, "code": "NSL", "name": "NorthStar Lines"},
    {"id": 2, "code": "BLX", "name": "Baltic Express"},
    {"id": 3, "code": "PCR", "name": "Pacific Reach"},
]


def make_settings(**overrides: object) -> Settings:
    values: dict[str, object] = {
        "booking_base_url": BOOKING,
        "pricing_base_url": PRICING,
        "gateway_timeout_seconds": 1.0,
        "gateway_retry_attempts": 3,
        "gateway_retry_base_delay_seconds": 0.0,
        "gateway_retry_max_delay_seconds": 0.0,
        "log_level": "WARNING",
    }
    values.update(overrides)
    return Settings(**values)  # type: ignore[arg-type]


@pytest.fixture
def upstream() -> Iterator[respx.MockRouter]:
    with respx.mock(assert_all_called=False) as router:
        yield router


@pytest.fixture
def settings() -> Settings:
    return make_settings()


@pytest.fixture
def client(upstream: respx.MockRouter, settings: Settings) -> Iterator[TestClient]:
    with TestClient(create_app(settings), raise_server_exceptions=False) as c:
        yield c


@pytest.fixture
def multi_client(upstream: respx.MockRouter) -> Iterator[TestClient]:
    app = create_app(make_settings(kestrel_feature_multi_carrier_quotes=True))
    with TestClient(app, raise_server_exceptions=False) as c:
        yield c


def json_response(body: object, status: int = 200, **kwargs: object) -> httpx.Response:
    return httpx.Response(status, json=body, **kwargs)  # type: ignore[arg-type]
