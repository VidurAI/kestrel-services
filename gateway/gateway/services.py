"""Gateway-level orchestration on top of the raw upstream clients."""

from __future__ import annotations

import asyncio

from gateway.cache import TTLCache
from gateway.clients.booking import BookingClient
from gateway.clients.pricing import PricingClient
from gateway.errors import BadRequestError, GatewayError, UpstreamBadGateway
from gateway.models import CarrierOut, QuoteIn, QuoteOut

_CARRIERS_KEY = "carriers"


class CarrierDirectory:
    """Booking's carrier list, cached briefly: it changes only on carrier onboarding."""

    def __init__(self, booking: BookingClient, ttl_seconds: float) -> None:
        self._booking = booking
        self._cache: TTLCache[str, list[CarrierOut]] = TTLCache(ttl_seconds, maxsize=4)

    @property
    def cache(self) -> TTLCache[str, list[CarrierOut]]:
        return self._cache

    async def list_carriers(self) -> tuple[list[CarrierOut], bool]:
        """Return ``(carriers, served_from_cache)``."""
        return await self._cache.get_or_load(_CARRIERS_KEY, self._booking.list_carriers)

    async def active_codes(self) -> list[str]:
        carriers, _ = await self.list_carriers()
        return [c.code for c in carriers if c.active]


class QuoteService:
    def __init__(
        self,
        pricing: PricingClient,
        directory: CarrierDirectory,
        *,
        multi_carrier_enabled: bool,
    ) -> None:
        self._pricing = pricing
        self._directory = directory
        self._multi_carrier = multi_carrier_enabled

    async def quote(self, request: QuoteIn) -> QuoteOut:
        if request.carrier_code:
            return await self._pricing.quote(request)
        if not self._multi_carrier:
            raise BadRequestError("carrier_code is required")
        return await self._cheapest(request)

    async def _cheapest(self, request: QuoteIn) -> QuoteOut:
        codes = await self._directory.active_codes()
        if not codes:
            raise UpstreamBadGateway("no active carriers available")

        results = await asyncio.gather(
            *(self._pricing.quote(request, carrier_code=code) for code in codes),
            return_exceptions=True,
        )
        quotes = [r for r in results if isinstance(r, QuoteOut)]
        if not quotes:
            errors = [r for r in results if isinstance(r, GatewayError)]
            if errors:
                raise errors[0]
            raise next(r for r in results if isinstance(r, BaseException))
        return min(quotes, key=lambda quote: quote.amount)
