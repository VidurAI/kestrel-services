"""Client for the pricing service (.NET, ``/v1`` API)."""

from __future__ import annotations

from gateway.clients.base import UpstreamClient
from gateway.errors import PricingUnavailable
from gateway.models import QuoteIn, QuoteOut, SurchargesOut


class PricingClient(UpstreamClient):
    service = "pricing"
    unavailable_error = PricingUnavailable

    async def quote(self, request: QuoteIn, *, carrier_code: str | None = None) -> QuoteOut:
        """Request a quote. ``carrier_code`` overrides the one on ``request``."""
        carrier = carrier_code or request.carrier_code
        payload = {
            "origin": request.origin,
            "destination": request.destination,
            "weight_kg": request.weight_kg,
            "carrier_code": carrier,
        }
        # A quote is a pure calculation, so POST is safe to retry.
        data = await self.request("POST", "/v1/quote", json=payload, idempotent=True)
        return self.parse(QuoteOut, data)

    async def surcharges(self, carrier_code: str) -> SurchargesOut:
        data = await self.request("GET", f"/v1/carriers/{carrier_code}/surcharges")
        if isinstance(data, list):
            data = {"carrier_code": carrier_code, "surcharges": data}
        elif isinstance(data, dict):
            data.setdefault("carrier_code", carrier_code)
        return self.parse(SurchargesOut, data)
