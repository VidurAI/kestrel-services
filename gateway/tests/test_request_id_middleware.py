from __future__ import annotations

import logging
import re

from gateway.logging_config import JsonFormatter
from tests.conftest import BOOKING, SHIPMENT, json_response


def test_request_id_is_generated(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(return_value=json_response(SHIPMENT))

    resp = client.get("/api/shipments/41")

    assert re.fullmatch(r"[0-9a-f]{32}", resp.headers["x-request-id"])


def test_inbound_request_id_is_echoed(client):
    resp = client.get("/healthz", headers={"X-Request-ID": "trace-12345678"})

    assert resp.headers["x-request-id"] == "trace-12345678"


def test_hostile_request_id_is_replaced(client):
    resp = client.get("/healthz", headers={"X-Request-ID": "x\"; drop table"})

    assert resp.headers["x-request-id"] != "x\"; drop table"
    assert re.fullmatch(r"[0-9a-f]{32}", resp.headers["x-request-id"])


def test_error_bodies_carry_the_same_request_id(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(return_value=json_response({"error": "gone"}, 404))

    resp = client.get("/api/shipments/41", headers={"X-Request-ID": "trace-abcdefgh"})

    assert resp.json()["request_id"] == "trace-abcdefgh"
    assert resp.headers["x-request-id"] == "trace-abcdefgh"


def test_access_log_line_is_emitted(client, caplog):
    logger = logging.getLogger("gateway.access")
    logger.addHandler(caplog.handler)
    try:
        with caplog.at_level(logging.INFO, logger="gateway.access"):
            client.get("/healthz")
    finally:
        logger.removeHandler(caplog.handler)

    record = next(r for r in caplog.records if r.name == "gateway.access")
    assert record.path == "/healthz" and record.status == 200
    assert record.duration_ms >= 0


def test_json_formatter_includes_request_id_and_extras():
    from gateway.context import set_request_id

    set_request_id("trace-json-0001")
    record = logging.LogRecord("gateway.x", logging.INFO, __file__, 1, "hello", None, None)
    record.status = 200
    try:
        out = JsonFormatter().format(record)
    finally:
        set_request_id(None)

    assert '"request_id": "trace-json-0001"' in out
    assert '"status": 200' in out
    assert '"msg": "hello"' in out
