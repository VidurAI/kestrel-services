"""Local pricing formula used before the pricing service existed.

The gateway computed quotes itself from a flat per-kg rate table, a distance band
derived from the region prefix of each location code, and a fixed fuel percentage.
"""

from __future__ import annotations

import math

# Rate per kg in EUR, by carrier.
BASE_RATE_PER_KG = {
    "NSL": 1.12,
    "BLX": 0.98,
    "PCR": 1.35,
}

# Distance bands keyed by (origin region, destination region); region = first 2 letters.
REGION_BAND = {
    ("NL", "NL"): 1.0,
    ("NL", "DE"): 1.15,
    ("NL", "PL"): 1.4,
    ("DE", "PL"): 1.25,
    ("NL", "US"): 2.6,
    ("DE", "US"): 2.5,
    ("PL", "US"): 2.8,
}

FUEL_PCT = 0.085
MIN_CHARGE = 24.0
VOLUME_DIVISOR = 167.0  # kg per cubic metre for chargeable weight


def _region(location: str) -> str:
    return location.strip().upper()[:2]


def _band(origin: str, destination: str) -> float:
    a, b = _region(origin), _region(destination)
    if (a, b) in REGION_BAND:
        return REGION_BAND[(a, b)]
    if (b, a) in REGION_BAND:
        return REGION_BAND[(b, a)]
    return 1.0 if a == b else 3.0


def chargeable_weight(weight_kg: float, volume_m3: float = 0.0) -> float:
    """Heavier of actual and volumetric weight, rounded up to the next 5 kg."""
    volumetric = volume_m3 * VOLUME_DIVISOR
    return math.ceil(max(weight_kg, volumetric) / 5.0) * 5.0


def quote(origin: str, destination: str, weight_kg: float, carrier_code: str,
          volume_m3: float = 0.0) -> dict:
    """Return ``{"amount", "currency", "breakdown"}`` for a shipment."""
    rate = BASE_RATE_PER_KG.get(carrier_code.upper())
    if rate is None:
        raise ValueError(f"unknown carrier: {carrier_code}")

    weight = chargeable_weight(weight_kg, volume_m3)
    freight = weight * rate * _band(origin, destination)
    freight = max(freight, MIN_CHARGE)
    fuel = round(freight * FUEL_PCT, 2)
    total = round(freight + fuel, 2)
    return {
        "amount": total,
        "currency": "EUR",
        "breakdown": [
            {"label": "freight", "amount": round(freight, 2)},
            {"label": "fuel", "amount": fuel},
        ],
    }
