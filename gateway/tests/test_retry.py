from __future__ import annotations

import httpx
import pytest

from gateway.clients.booking import BookingClient
from gateway.errors import BookingUnavailable, UpstreamTimeout
from gateway.retry import RetryPolicy, is_retryable_error, retry_async
from tests.conftest import BOOKING, SHIPMENT, json_response


def test_delay_grows_exponentially_and_is_capped():
    policy = RetryPolicy(attempts=5, base_delay=0.1, max_delay=0.5, jitter=0.0)

    assert [policy.delay_for(n) for n in (1, 2, 3, 4)] == [0.1, 0.2, 0.4, 0.5]


def test_jitter_stays_within_bounds():
    policy = RetryPolicy(base_delay=1.0, max_delay=10.0, jitter=0.25)

    assert policy.delay_for(1, rng=lambda: 0.0) == pytest.approx(0.75)
    assert policy.delay_for(1, rng=lambda: 1.0) == pytest.approx(1.25)


def test_zero_delay_policy_never_sleeps():
    assert RetryPolicy(base_delay=0.0).delay_for(3) == 0.0


def test_retry_classification():
    assert is_retryable_error(httpx.ConnectError("x"), idempotent=False)
    assert not is_retryable_error(httpx.ReadTimeout("x"), idempotent=False)
    assert is_retryable_error(httpx.ReadTimeout("x"), idempotent=True)
    assert not is_retryable_error(ValueError("x"), idempotent=True)


async def test_retry_async_returns_after_transient_failures():
    sleeps: list[float] = []
    attempts = 0

    async def flaky() -> str:
        nonlocal attempts
        attempts += 1
        if attempts < 3:
            raise httpx.ConnectError("down")
        return "ok"

    async def fake_sleep(delay: float) -> None:
        sleeps.append(delay)

    result = await retry_async(
        flaky,
        policy=RetryPolicy(attempts=3, base_delay=0.1, jitter=0.0),
        should_retry=lambda exc: is_retryable_error(exc, idempotent=True),
        sleep=fake_sleep,
    )

    assert result == "ok"
    assert sleeps == [0.1, 0.2]


async def test_retry_async_gives_up_after_max_attempts():
    attempts = 0

    async def always_down() -> None:
        nonlocal attempts
        attempts += 1
        raise httpx.ConnectError("down")

    async def no_sleep(_: float) -> None:
        return None

    with pytest.raises(httpx.ConnectError):
        await retry_async(
            always_down,
            policy=RetryPolicy(attempts=4),
            should_retry=lambda exc: True,
            sleep=no_sleep,
        )
    assert attempts == 4


async def test_non_retryable_error_raises_immediately():
    attempts = 0

    async def broken() -> None:
        nonlocal attempts
        attempts += 1
        raise ValueError("bug")

    with pytest.raises(ValueError):
        await retry_async(broken, policy=RetryPolicy(), should_retry=lambda exc: False)
    assert attempts == 1


@pytest.fixture
async def booking(upstream):
    async def no_sleep(_: float) -> None:
        return None

    async with httpx.AsyncClient(base_url=BOOKING) as http:
        yield BookingClient(http, policy=RetryPolicy(attempts=3, base_delay=0.0), sleep=no_sleep)


async def test_get_retries_503_then_succeeds(booking, upstream):
    route = upstream.get(f"{BOOKING}/v2/shipments/41")
    route.side_effect = [json_response({}, 503), json_response({}, 502), json_response(SHIPMENT)]

    shipment = await booking.get_shipment("41")

    assert shipment.reference == "KST-2026-0041"
    assert route.call_count == 3


async def test_get_exhausts_retries_and_reports_unavailable(booking, upstream):
    route = upstream.get(f"{BOOKING}/v2/shipments/41").mock(return_value=json_response({}, 503))

    with pytest.raises(BookingUnavailable):
        await booking.get_shipment("41")
    assert route.call_count == 3


async def test_reserve_is_not_retried_on_503(booking, upstream):
    route = upstream.post(f"{BOOKING}/v2/shipments/41/reserve").mock(return_value=json_response({}, 503))

    with pytest.raises(BookingUnavailable):
        await booking.reserve_shipment("41")
    assert route.call_count == 1


async def test_reserve_is_retried_on_connect_error(booking, upstream):
    route = upstream.post(f"{BOOKING}/v2/shipments/41/reserve")
    route.side_effect = [httpx.ConnectError("refused"), json_response(SHIPMENT)]

    assert (await booking.reserve_shipment("41")).id == 41
    assert route.call_count == 2


async def test_read_timeout_on_get_retries_then_maps_to_504(booking, upstream):
    route = upstream.get(f"{BOOKING}/v2/shipments/41").mock(side_effect=httpx.ReadTimeout("slow"))

    with pytest.raises(UpstreamTimeout):
        await booking.get_shipment("41")
    assert route.call_count == 3
