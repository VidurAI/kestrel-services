"""Carrier directory (booking) and surcharge lookup (pricing)."""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, Path, Response

from gateway.clients.pricing import PricingClient
from gateway.deps import get_carrier_directory, get_pricing
from gateway.models import CarrierOut, SurchargesOut
from gateway.services import CarrierDirectory

router = APIRouter(prefix="/carriers", tags=["carriers"])

CarrierCodePath = Annotated[str, Path(pattern=r"^[A-Za-z]{3}$")]


@router.get("", response_model=list[CarrierOut])
async def list_carriers(
    response: Response, directory: Annotated[CarrierDirectory, Depends(get_carrier_directory)]
) -> list[CarrierOut]:
    carriers, cached = await directory.list_carriers()
    response.headers["X-Cache"] = "HIT" if cached else "MISS"
    return carriers


@router.get("/{code}/surcharges", response_model=SurchargesOut)
async def carrier_surcharges(
    code: CarrierCodePath, pricing: Annotated[PricingClient, Depends(get_pricing)]
) -> SurchargesOut:
    return await pricing.surcharges(code.upper())
