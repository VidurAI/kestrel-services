"""FastAPI dependencies resolving shared objects from ``app.state``."""

from __future__ import annotations

from fastapi import Request

from gateway.clients.booking import BookingClient
from gateway.clients.pricing import PricingClient
from gateway.config import Settings
from gateway.services import CarrierDirectory, QuoteService


def get_settings_dep(request: Request) -> Settings:
    return request.app.state.settings


def get_booking(request: Request) -> BookingClient:
    return request.app.state.booking


def get_pricing(request: Request) -> PricingClient:
    return request.app.state.pricing


def get_carrier_directory(request: Request) -> CarrierDirectory:
    return request.app.state.carriers


def get_quote_service(request: Request) -> QuoteService:
    return request.app.state.quotes
