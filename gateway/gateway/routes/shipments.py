"""``/api/shipments`` routes: thin pass-throughs to booking's ``/v2/shipments``."""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, Path, Response, status

from gateway.clients.booking import BookingClient
from gateway.deps import get_booking
from gateway.models import ShipmentIn, ShipmentOut

router = APIRouter(prefix="/shipments", tags=["shipments"])

ShipmentId = Annotated[str, Path(pattern=r"^[A-Za-z0-9_-]{1,64}$")]
Booking = Annotated[BookingClient, Depends(get_booking)]


@router.post("", response_model=ShipmentOut, status_code=status.HTTP_201_CREATED)
async def create_shipment(payload: ShipmentIn, response: Response, booking: Booking) -> ShipmentOut:
    shipment = await booking.create_shipment(payload)
    response.headers["Location"] = f"/api/shipments/{shipment.id}"
    return shipment


@router.get("/{shipment_id}", response_model=ShipmentOut)
async def get_shipment(shipment_id: ShipmentId, booking: Booking) -> ShipmentOut:
    return await booking.get_shipment(shipment_id)


@router.post("/{shipment_id}/reserve", response_model=ShipmentOut)
async def reserve_shipment(shipment_id: ShipmentId, booking: Booking) -> ShipmentOut:
    return await booking.reserve_shipment(shipment_id)
