"""Client for the booking service (Java, ``/v2`` API)."""

from __future__ import annotations

import httpx

from gateway.clients.base import UpstreamClient
from gateway.errors import BookingUnavailable, GatewayError, UpstreamBadGateway
from gateway.models import CarrierOut, ShipmentIn, ShipmentOut

CAPACITY_EXHAUSTED_MARKER = "carrier capacity exhausted"


class BookingClient(UpstreamClient):
    service = "booking"
    unavailable_error = BookingUnavailable

    def translate_status(self, response: httpx.Response) -> GatewayError | None:
        # Booking answers 429 when a carrier's token bucket is empty. Callers of the
        # public API should see a plain 503 rather than booking's internal throttling.
        if response.status_code == 429 and CAPACITY_EXHAUSTED_MARKER in response.text.lower():
            retry_after = response.headers.get("Retry-After")
            headers = {"Retry-After": retry_after} if retry_after else None
            return BookingUnavailable(headers=headers, extra={"reason": "capacity_exhausted"})
        return None

    def counts_as_failure(self, error: GatewayError) -> bool:
        # Capacity exhaustion is a healthy upstream saying "slow down", not an outage.
        if error.extra.get("reason") == "capacity_exhausted":
            return False
        return super().counts_as_failure(error)

    async def create_shipment(self, payload: ShipmentIn) -> ShipmentOut:
        # Booking references are idempotent, so a call carrying one is safe to retry.
        data = await self.request(
            "POST",
            "/v2/shipments",
            json=payload.model_dump(exclude_none=True),
            idempotent=payload.reference is not None,
        )
        return self.parse(ShipmentOut, data)

    async def get_shipment(self, shipment_id: str) -> ShipmentOut:
        data = await self.request("GET", f"/v2/shipments/{shipment_id}")
        return self.parse(ShipmentOut, data)

    async def reserve_shipment(self, shipment_id: str) -> ShipmentOut:
        data = await self.request("POST", f"/v2/shipments/{shipment_id}/reserve", idempotent=False)
        return self.parse(ShipmentOut, data)

    async def list_carriers(self) -> list[CarrierOut]:
        data = await self.request("GET", "/v2/carriers")
        if not isinstance(data, list):
            data = data.get("carriers") if isinstance(data, dict) else None
        if not isinstance(data, list):
            raise UpstreamBadGateway("invalid response from booking")
        return [self.parse(CarrierOut, item) for item in data]
