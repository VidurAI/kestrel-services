from __future__ import annotations

import json

from tests.conftest import BOOKING, CARRIERS, PRICING, QUOTE, json_response

REQ = {"origin": "NLRTM", "destination": "DEHAM", "weight_kg": 500, "carrier_code": "NSL"}


def test_quote_mirrors_pricing_response(client, upstream):
    route = upstream.post(f"{PRICING}/v1/quote").mock(return_value=json_response(QUOTE))

    resp = client.post("/api/quote", json=REQ)

    assert resp.status_code == 200
    assert resp.json() == QUOTE
    assert json.loads(route.calls.last.request.content) == REQ


def test_quote_normalises_carrier_code(client, upstream):
    route = upstream.post(f"{PRICING}/v1/quote").mock(return_value=json_response(QUOTE))

    client.post("/api/quote", json={**REQ, "carrier_code": " blx "})

    assert json.loads(route.calls.last.request.content)["carrier_code"] == "BLX"


def test_unknown_carrier_is_422(client, upstream):
    upstream.post(f"{PRICING}/v1/quote").mock(
        return_value=json_response({"error": "unknown carrier code"}, 422)
    )

    resp = client.post("/api/quote", json={**REQ, "carrier_code": "ZZZ"})

    assert resp.status_code == 422
    assert resp.json()["detail"] == "unknown carrier code"
    assert resp.json()["code"] == "upstream_rejected"


def test_weight_must_be_positive(client, upstream):
    resp = client.post("/api/quote", json={**REQ, "weight_kg": 0})

    assert resp.status_code == 422
    assert resp.json()["code"] == "validation_error"


def test_missing_carrier_requires_feature_flag(client, upstream):
    route = upstream.post(f"{PRICING}/v1/quote").mock(return_value=json_response(QUOTE))
    body = {k: v for k, v in REQ.items() if k != "carrier_code"}

    resp = client.post("/api/quote", json=body)

    assert resp.status_code == 422
    assert resp.json()["detail"] == "carrier_code is required"
    assert not route.called


def test_multi_carrier_returns_cheapest(multi_client, upstream):
    upstream.get(f"{BOOKING}/v2/carriers").mock(return_value=json_response(CARRIERS))
    prices = {"NSL": 128.4, "BLX": 99.9, "PCR": 150.0}

    def respond(request):
        code = json.loads(request.content)["carrier_code"]
        return json_response({**QUOTE, "quote_id": f"q-{code}", "amount": prices[code]})

    upstream.post(f"{PRICING}/v1/quote").mock(side_effect=respond)
    body = {k: v for k, v in REQ.items() if k != "carrier_code"}

    resp = multi_client.post("/api/quote", json=body)

    assert resp.status_code == 200
    assert resp.json()["quote_id"] == "q-BLX"
    assert resp.json()["amount"] == 99.9


def test_multi_carrier_tolerates_partial_failure(multi_client, upstream):
    upstream.get(f"{BOOKING}/v2/carriers").mock(return_value=json_response(CARRIERS))

    def respond(request):
        code = json.loads(request.content)["carrier_code"]
        if code == "BLX":
            return json_response({"error": "unknown carrier code"}, 422)
        return json_response({**QUOTE, "quote_id": f"q-{code}", "amount": 100.0 if code == "PCR" else 120.0})

    upstream.post(f"{PRICING}/v1/quote").mock(side_effect=respond)
    body = {k: v for k, v in REQ.items() if k != "carrier_code"}

    resp = multi_client.post("/api/quote", json=body)

    assert resp.status_code == 200
    assert resp.json()["quote_id"] == "q-PCR"


def test_multi_carrier_all_failing_surfaces_first_error(multi_client, upstream):
    upstream.get(f"{BOOKING}/v2/carriers").mock(return_value=json_response(CARRIERS))
    upstream.post(f"{PRICING}/v1/quote").mock(return_value=json_response({"error": "boom"}, 500))
    body = {k: v for k, v in REQ.items() if k != "carrier_code"}

    resp = multi_client.post("/api/quote", json=body)

    assert resp.status_code == 502


def test_pricing_outage_is_503(client, upstream):
    upstream.post(f"{PRICING}/v1/quote").mock(return_value=json_response({}, 503))

    resp = client.post("/api/quote", json=REQ)

    assert resp.status_code == 503
    assert resp.json()["detail"] == "pricing temporarily unavailable"
