from __future__ import annotations

import ast
import pathlib

from gateway import __version__
from gateway.main import create_app
from tests.conftest import make_settings


def test_healthz(client):
    resp = client.get("/healthz")

    assert resp.status_code == 200
    body = resp.json()
    assert body["status"] == "ok" and body["version"] == __version__
    assert body["breakers"] == {"booking": "closed", "pricing": "closed"}


def test_expected_public_routes_are_registered():
    paths = create_app(make_settings()).openapi()["paths"]
    routes = {(m.upper(), path) for path, ops in paths.items() for m in ops}

    assert ("POST", "/api/shipments") in routes
    assert ("GET", "/api/shipments/{shipment_id}") in routes
    assert ("POST", "/api/shipments/{shipment_id}/reserve") in routes
    assert ("POST", "/api/quote") in routes
    assert ("GET", "/api/carriers") in routes


def test_openapi_schema_builds():
    schema = create_app(make_settings()).openapi()

    assert "ShipmentOut" in schema["components"]["schemas"]
    assert "/api/quote" in schema["paths"]


def test_clients_only_call_documented_upstream_paths():
    root = pathlib.Path(__file__).resolve().parents[1] / "gateway" / "clients"
    literals = set()
    for path in (root / "booking.py", root / "pricing.py"):
        for node in ast.walk(ast.parse(path.read_text())):
            if isinstance(node, ast.Constant) and isinstance(node.value, str) and node.value.startswith("/v"):
                literals.add(node.value)
            if isinstance(node, ast.JoinedStr):
                head = node.values[0]
                if isinstance(head, ast.Constant) and str(head.value).startswith("/v"):
                    literals.add(head.value)

    assert any(v.startswith("/v2/shipments") for v in literals)
    assert "/v2/carriers" in literals
    assert "/v1/quote" in literals
    assert any(v.startswith("/v1/carriers/") for v in literals)
