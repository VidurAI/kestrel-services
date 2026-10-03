"""Booking's per-carrier throttling must never leak out as a 429."""

from __future__ import annotations

import httpx

from tests.conftest import BOOKING, json_response

NEW = {"origin": "NLRTM", "destination": "DEHAM", "carrier_code": "NSL"}


def test_capacity_exhausted_429_becomes_503_on_create(client, upstream):
    route = upstream.post(f"{BOOKING}/v2/shipments").mock(
        return_value=json_response({"error": "carrier capacity exhausted", "carrier": "NSL"}, 429)
    )

    resp = client.post("/api/shipments", json=NEW)

    assert resp.status_code == 503
    body = resp.json()
    assert body["detail"] == "booking temporarily unavailable"
    assert body["code"] == "booking_unavailable"
    assert body["request_id"] == resp.headers["x-request-id"]
    assert route.call_count == 1  # throttled calls are not retried


def test_capacity_exhausted_on_reserve(client, upstream):
    upstream.post(f"{BOOKING}/v2/shipments/41/reserve").mock(
        return_value=httpx.Response(429, text="Carrier Capacity Exhausted for NSL")
    )

    resp = client.post("/api/shipments/41/reserve")

    assert resp.status_code == 503
    assert resp.json()["detail"] == "booking temporarily unavailable"


def test_retry_after_is_preserved(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(
        return_value=json_response({"error": "carrier capacity exhausted"}, 429, headers={"Retry-After": "3"})
    )

    resp = client.get("/api/shipments/41")

    assert resp.status_code == 503
    assert resp.headers["retry-after"] == "3"


def test_other_429_is_not_masked(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(
        return_value=json_response({"error": "slow down"}, 429, headers={"Retry-After": "1"})
    )

    resp = client.get("/api/shipments/41")

    assert resp.status_code == 429
    assert resp.json()["detail"] == "slow down"
    assert resp.json()["code"] == "rate_limited"


def test_capacity_errors_do_not_trip_the_breaker(upstream):
    from fastapi.testclient import TestClient

    from gateway.main import create_app
    from tests.conftest import make_settings

    upstream.get(f"{BOOKING}/v2/shipments/41").mock(
        return_value=json_response({"error": "carrier capacity exhausted"}, 429)
    )
    app = create_app(make_settings(gateway_breaker_failure_threshold=2))
    with TestClient(app, raise_server_exceptions=False) as client:
        for _ in range(4):
            assert client.get("/api/shipments/41").status_code == 503
        assert client.get("/healthz").json()["breakers"]["booking"] == "closed"
