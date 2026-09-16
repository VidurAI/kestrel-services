"""Application factory and ASGI entrypoint (``uvicorn gateway.main:app``)."""

from __future__ import annotations

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager

import httpx
from fastapi import FastAPI

from gateway import __version__
from gateway.breaker import CircuitBreaker
from gateway.clients.booking import BookingClient
from gateway.clients.pricing import PricingClient
from gateway.config import Settings, get_settings
from gateway.errors import install_error_handlers
from gateway.logging_config import configure_logging
from gateway.middleware import AccessLogMiddleware, RequestIdMiddleware
from gateway.retry import RetryPolicy
from gateway.routes import build_router
from gateway.services import CarrierDirectory, QuoteService


def _http_client(base_url: str, timeout: float) -> httpx.AsyncClient:
    return httpx.AsyncClient(
        base_url=base_url,
        timeout=httpx.Timeout(timeout, connect=min(timeout, 2.0)),
        headers={"User-Agent": f"kestrel-gateway/{__version__}", "Accept": "application/json"},
    )


def _breaker(name: str, settings: Settings) -> CircuitBreaker:
    return CircuitBreaker(
        name,
        failure_threshold=settings.gateway_breaker_failure_threshold,
        reset_after=settings.gateway_breaker_reset_seconds,
    )


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or get_settings()
    configure_logging(settings.log_level)

    @asynccontextmanager
    async def lifespan(app: FastAPI) -> AsyncIterator[None]:
        policy = RetryPolicy(
            attempts=settings.gateway_retry_attempts,
            base_delay=settings.gateway_retry_base_delay_seconds,
            max_delay=settings.gateway_retry_max_delay_seconds,
        )
        timeout = settings.gateway_timeout_seconds
        async with (
            _http_client(settings.booking_base_url, timeout) as booking_http,
            _http_client(settings.pricing_base_url, timeout) as pricing_http,
        ):
            booking = BookingClient(booking_http, policy=policy, breaker=_breaker("booking", settings))
            pricing = PricingClient(pricing_http, policy=policy, breaker=_breaker("pricing", settings))
            directory = CarrierDirectory(booking, settings.gateway_carriers_cache_ttl_seconds)
            app.state.booking = booking
            app.state.pricing = pricing
            app.state.carriers = directory
            app.state.quotes = QuoteService(
                pricing,
                directory,
                multi_carrier_enabled=settings.kestrel_feature_multi_carrier_quotes,
            )
            yield

    app = FastAPI(title="Kestrel Gateway", version=__version__, lifespan=lifespan)
    app.state.settings = settings
    install_error_handlers(app)
    app.include_router(build_router())
    # Added last = outermost: the request id must exist before the access log runs.
    app.add_middleware(AccessLogMiddleware)
    app.add_middleware(RequestIdMiddleware)
    return app


app = create_app()
