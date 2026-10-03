from __future__ import annotations

import httpx
from fastapi.testclient import TestClient

from gateway.main import create_app
from tests.conftest import BOOKING, PRICING, json_response, make_settings


def test_unknown_route_uses_structured_404(client):
    resp = client.get("/api/nope")

    assert resp.status_code == 404
    body = resp.json()
    assert body["code"] == "not_found"
    assert set(body) == {"detail", "code", "request_id"}


def test_validation_error_lists_fields(client):
    resp = client.post("/api/quote", json={"origin": "NLRTM"})

    assert resp.status_code == 422
    body = resp.json()
    assert body["detail"] == "request validation failed"
    missing = {tuple(e["loc"]) for e in body["errors"]}
    assert ("body", "destination") in missing and ("body", "weight_kg") in missing


def test_method_not_allowed_is_structured(client):
    resp = client.delete("/api/carriers")

    assert resp.status_code == 405
    assert resp.json()["code"] == "http_error"


def test_unhandled_exception_is_hidden_behind_500(upstream):
    app = create_app(make_settings())

    @app.get("/boom")
    async def boom() -> None:
        raise RuntimeError("secret internal detail")

    with TestClient(app, raise_server_exceptions=False) as client:
        resp = client.get("/boom")

    assert resp.status_code == 500
    assert resp.json()["detail"] == "internal server error"
    assert "secret" not in resp.text
    assert resp.json()["request_id"]


def test_connection_refused_maps_to_503(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(side_effect=httpx.ConnectError("refused"))

    resp = client.get("/api/shipments/41")

    assert resp.status_code == 503
    assert resp.json()["detail"] == "booking temporarily unavailable"


def test_upstream_timeout_maps_to_504(client, upstream):
    upstream.post(f"{PRICING}/v1/quote").mock(side_effect=httpx.ReadTimeout("slow"))

    resp = client.post(
        "/api/quote", json={"origin": "NLRTM", "destination": "DEHAM", "weight_kg": 5, "carrier_code": "NSL"}
    )

    assert resp.status_code == 504
    assert resp.json()["code"] == "upstream_timeout"


def test_malformed_upstream_json_is_502(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(return_value=httpx.Response(200, text="<html>"))

    resp = client.get("/api/shipments/41")

    assert resp.status_code == 502
    assert resp.json()["detail"] == "booking returned malformed JSON"


def test_unexpected_upstream_status_is_502(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(return_value=json_response({"error": "teapot"}, 418))

    assert client.get("/api/shipments/41").status_code == 502
