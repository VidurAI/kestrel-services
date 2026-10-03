from __future__ import annotations

from tests.conftest import BOOKING, SHIPMENT, json_response

NEW = {"origin": "NLRTM", "destination": "DEHAM", "carrier_code": "nsl", "weight_kg": 820}


def test_create_shipment_proxies_to_booking(client, upstream):
    route = upstream.post(f"{BOOKING}/v2/shipments").mock(return_value=json_response(SHIPMENT, 201))

    resp = client.post("/api/shipments", json=NEW)

    assert resp.status_code == 201
    body = resp.json()
    assert body["id"] == 41
    assert body["reference"] == "KST-2026-0041"
    assert body["carrier_code"] == "NSL"
    assert body["eta"] == "2026-11-02T10:00:00Z"
    assert resp.headers["location"] == "/api/shipments/41"
    # carrier code is normalised before it reaches booking, None fields are dropped
    sent = route.calls.last.request.read()
    assert b'"carrier_code":"NSL"' in sent.replace(b" ", b"")
    assert b"reference" not in sent


def test_create_shipment_validates_payload(client, upstream):
    route = upstream.post(f"{BOOKING}/v2/shipments").mock(return_value=json_response(SHIPMENT, 201))

    resp = client.post("/api/shipments", json={"origin": "N", "destination": "DEHAM", "carrier_code": "NSL"})

    assert resp.status_code == 422
    assert resp.json()["code"] == "validation_error"
    assert not route.called


def test_shipment_out_has_exact_public_fields(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(return_value=json_response(SHIPMENT))

    body = client.get("/api/shipments/41").json()

    assert set(body) == {"id", "reference", "origin", "destination", "carrier_code", "status", "eta"}


def test_get_shipment(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(return_value=json_response(SHIPMENT))

    resp = client.get("/api/shipments/41")

    assert resp.status_code == 200
    assert resp.json()["status"] == "CREATED"


def test_get_shipment_not_found(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/999").mock(
        return_value=json_response({"error": "shipment 999 not found"}, 404)
    )

    resp = client.get("/api/shipments/999")

    assert resp.status_code == 404
    assert resp.json()["detail"] == "shipment 999 not found"
    assert resp.json()["code"] == "not_found"


def test_get_shipment_rejects_odd_ids_without_calling_upstream(client, upstream):
    route = upstream.get(url__startswith=BOOKING).mock(return_value=json_response(SHIPMENT))

    resp = client.get("/api/shipments/4%201..")

    assert resp.status_code == 422
    assert not route.called


def test_reserve_shipment(client, upstream):
    reserved = {**SHIPMENT, "status": "RESERVED"}
    route = upstream.post(f"{BOOKING}/v2/shipments/41/reserve").mock(return_value=json_response(reserved))

    resp = client.post("/api/shipments/41/reserve")

    assert resp.status_code == 200
    assert resp.json()["status"] == "RESERVED"
    assert route.call_count == 1


def test_reserve_conflict_is_passed_through(client, upstream):
    upstream.post(f"{BOOKING}/v2/shipments/41/reserve").mock(
        return_value=json_response({"message": "shipment already reserved"}, 409)
    )

    resp = client.post("/api/shipments/41/reserve")

    assert resp.status_code == 409
    assert resp.json()["detail"] == "shipment already reserved"


def test_malformed_booking_payload_becomes_502(client, upstream):
    upstream.get(f"{BOOKING}/v2/shipments/41").mock(return_value=json_response({"id": 41}))

    resp = client.get("/api/shipments/41")

    assert resp.status_code == 502
    assert resp.json()["detail"] == "invalid response from booking"


def test_request_id_is_forwarded_to_booking(client, upstream):
    route = upstream.get(f"{BOOKING}/v2/shipments/41").mock(return_value=json_response(SHIPMENT))

    client.get("/api/shipments/41", headers={"X-Request-ID": "req-abcdef123"})

    assert route.calls.last.request.headers["x-request-id"] == "req-abcdef123"
