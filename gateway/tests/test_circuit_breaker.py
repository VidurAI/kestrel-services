from __future__ import annotations

import httpx
import pytest
from fastapi.testclient import TestClient

from gateway.breaker import BreakerState, CircuitBreaker
from gateway.main import create_app
from tests.conftest import BOOKING, SHIPMENT, json_response, make_settings


class Clock:
    now = 0.0

    def __call__(self) -> float:
        return self.now


def make(clock: Clock) -> CircuitBreaker:
    return CircuitBreaker("t", failure_threshold=3, reset_after=10, clock=clock)


def test_opens_after_threshold():
    clock = Clock()
    b = make(clock)
    for _ in range(2):
        b.record_failure()
    assert b.state is BreakerState.CLOSED and b.allow()

    b.record_failure()

    assert b.state is BreakerState.OPEN
    assert not b.allow()


def test_success_resets_failure_count():
    b = make(Clock())
    b.record_failure()
    b.record_failure()
    b.record_success()
    b.record_failure()
    b.record_failure()

    assert b.state is BreakerState.CLOSED


def test_half_open_admits_a_single_probe():
    clock = Clock()
    b = make(clock)
    for _ in range(3):
        b.record_failure()
    clock.now = 10

    assert b.state is BreakerState.HALF_OPEN
    assert b.allow() is True
    assert b.allow() is False


def test_successful_probe_closes():
    clock = Clock()
    b = make(clock)
    for _ in range(3):
        b.record_failure()
    clock.now = 11
    assert b.allow()

    b.record_success()

    assert b.state is BreakerState.CLOSED and b.allow()


def test_failed_probe_reopens_for_a_full_window():
    clock = Clock()
    b = make(clock)
    for _ in range(3):
        b.record_failure()
    clock.now = 11
    assert b.allow()

    b.record_failure()

    assert b.state is BreakerState.OPEN
    clock.now = 20
    assert b.state is BreakerState.OPEN
    clock.now = 21.5
    assert b.state is BreakerState.HALF_OPEN


def test_gateway_fails_fast_once_booking_breaker_is_open(upstream):
    route = upstream.get(f"{BOOKING}/v2/shipments/41").mock(side_effect=httpx.ConnectError("refused"))
    app = create_app(make_settings(gateway_breaker_failure_threshold=2, gateway_retry_attempts=1))

    with TestClient(app, raise_server_exceptions=False) as client:
        assert client.get("/api/shipments/41").status_code == 503
        assert client.get("/api/shipments/41").status_code == 503
        calls_before = route.call_count
        resp = client.get("/api/shipments/41")
        health = client.get("/healthz").json()

    assert resp.status_code == 503
    assert resp.json()["detail"] == "booking temporarily unavailable"
    assert route.call_count == calls_before  # short-circuited, upstream untouched
    assert health["breakers"]["booking"] == "open"
    assert health["breakers"]["pricing"] == "closed"


def test_breaker_recovers_after_reset_window(upstream):
    route = upstream.get(f"{BOOKING}/v2/shipments/41")
    route.side_effect = [httpx.ConnectError("x"), json_response(SHIPMENT)]
    app = create_app(
        make_settings(
            gateway_breaker_failure_threshold=1, gateway_retry_attempts=1, gateway_breaker_reset_seconds=0.01
        )
    )
    import time

    with TestClient(app, raise_server_exceptions=False) as client:
        assert client.get("/api/shipments/41").status_code == 503
        time.sleep(0.05)
        assert client.get("/api/shipments/41").status_code == 200
        assert client.get("/healthz").json()["breakers"]["booking"] == "closed"


@pytest.mark.parametrize("threshold", [0, -1])
def test_invalid_threshold_rejected_by_settings(threshold):
    with pytest.raises(Exception):
        make_settings(gateway_breaker_failure_threshold=threshold)
