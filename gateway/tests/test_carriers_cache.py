from __future__ import annotations

import asyncio

from gateway.cache import TTLCache
from tests.conftest import BOOKING, CARRIERS, PRICING, json_response


class FakeClock:
    def __init__(self) -> None:
        self.now = 1000.0

    def __call__(self) -> float:
        return self.now


def test_carriers_listing(client, upstream):
    upstream.get(f"{BOOKING}/v2/carriers").mock(return_value=json_response(CARRIERS))

    resp = client.get("/api/carriers")

    assert resp.status_code == 200
    assert [c["code"] for c in resp.json()] == ["NSL", "BLX", "PCR"]
    assert resp.json()[1] == {"code": "BLX", "name": "Baltic Express", "active": True}


def test_second_listing_is_served_from_cache(client, upstream):
    route = upstream.get(f"{BOOKING}/v2/carriers").mock(return_value=json_response(CARRIERS))

    first = client.get("/api/carriers")
    second = client.get("/api/carriers")

    assert first.headers["x-cache"] == "MISS"
    assert second.headers["x-cache"] == "HIT"
    assert route.call_count == 1
    assert first.json() == second.json()


def test_upstream_failure_is_not_cached(client, upstream):
    route = upstream.get(f"{BOOKING}/v2/carriers")
    route.side_effect = [
        json_response({}, 500),
        json_response(CARRIERS),
    ]

    assert client.get("/api/carriers").status_code == 502
    ok = client.get("/api/carriers")

    assert ok.status_code == 200
    assert ok.headers["x-cache"] == "MISS"


def test_carriers_accepts_wrapped_payload(client, upstream):
    upstream.get(f"{BOOKING}/v2/carriers").mock(return_value=json_response({"carriers": CARRIERS}))

    assert len(client.get("/api/carriers").json()) == 3


def test_surcharges_route(client, upstream):
    route = upstream.get(f"{PRICING}/v1/carriers/NSL/surcharges").mock(
        return_value=json_response([{"code": "FUEL", "pct": 8.5}, {"code": "PEAK", "amount": 12.0}])
    )

    resp = client.get("/api/carriers/nsl/surcharges")

    assert resp.status_code == 200
    body = resp.json()
    assert body["carrier_code"] == "NSL"
    assert body["surcharges"][0] == {"code": "FUEL", "label": None, "pct": 8.5, "amount": None}
    assert route.call_count == 1


def test_surcharges_rejects_bad_code(client, upstream):
    assert client.get("/api/carriers/NSLX/surcharges").status_code == 422


def test_ttl_cache_expires_entries():
    clock = FakeClock()
    cache: TTLCache[str, int] = TTLCache(10, clock=clock)
    cache.set("a", 1)

    assert cache.get("a") == 1
    clock.now += 9.9
    assert cache.get("a") == 1
    clock.now += 0.2
    assert cache.get("a") is None
    assert len(cache) == 0


def test_ttl_cache_evicts_least_recently_used():
    cache: TTLCache[str, int] = TTLCache(60, maxsize=2)
    cache.set("a", 1)
    cache.set("b", 2)
    cache.get("a")
    cache.set("c", 3)

    assert cache.get("b") is None
    assert cache.get("a") == 1
    assert cache.get("c") == 3


async def test_get_or_load_is_single_flight():
    cache: TTLCache[str, list[int]] = TTLCache(60)
    calls = 0

    async def loader() -> list[int]:
        nonlocal calls
        calls += 1
        await asyncio.sleep(0.01)
        return []

    results = await asyncio.gather(*(cache.get_or_load("k", loader) for _ in range(5)))

    assert calls == 1
    assert sorted(hit for _, hit in results) == [False, True, True, True, True]
    assert (cache.hits, cache.misses) == (4, 1)


def test_invalidate_one_and_all():
    cache: TTLCache[str, int] = TTLCache(60)
    cache.set("a", 1)
    cache.set("b", 2)
    cache.invalidate("a")
    assert cache.get("a") is None and cache.get("b") == 2
    cache.invalidate()
    assert len(cache) == 0
