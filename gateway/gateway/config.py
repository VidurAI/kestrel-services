"""Runtime configuration for the gateway, loaded from the environment.

Environment variables (case-insensitive) map 1:1 onto the field names below,
e.g. ``BOOKING_BASE_URL`` -> ``booking_base_url``.
"""

from __future__ import annotations

from functools import lru_cache

from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(case_sensitive=False, extra="ignore")

    # Upstream services.
    booking_base_url: str = "http://localhost:8081"
    pricing_base_url: str = "http://localhost:8082"

    # Feature flags (shared naming scheme across the Kestrel services).
    kestrel_feature_multi_carrier_quotes: bool = False

    # Total per-request budget for a single upstream call, in seconds.
    gateway_timeout_seconds: float = 5.0

    # Retry/backoff for upstream calls.
    gateway_retry_attempts: int = 3
    gateway_retry_base_delay_seconds: float = 0.2
    gateway_retry_max_delay_seconds: float = 2.0

    # Circuit breaker guarding each upstream.
    gateway_breaker_failure_threshold: int = 5
    gateway_breaker_reset_seconds: float = 15.0

    # Carrier directory cache.
    gateway_carriers_cache_ttl_seconds: float = 60.0

    log_level: str = "INFO"

    @field_validator("booking_base_url", "pricing_base_url")
    @classmethod
    def _normalise_base_url(cls, value: str) -> str:
        value = value.strip().rstrip("/")
        if not value.startswith(("http://", "https://")):
            raise ValueError("base url must start with http:// or https://")
        return value

    @field_validator("gateway_timeout_seconds")
    @classmethod
    def _positive_timeout(cls, value: float) -> float:
        if value <= 0:
            raise ValueError("GATEWAY_TIMEOUT_SECONDS must be > 0")
        return value

    @field_validator("gateway_retry_attempts", "gateway_breaker_failure_threshold")
    @classmethod
    def _at_least_one(cls, value: int) -> int:
        if value < 1:
            raise ValueError("value must be >= 1")
        return value

    @field_validator("log_level")
    @classmethod
    def _upper_level(cls, value: str) -> str:
        return value.strip().upper()


@lru_cache
def get_settings() -> Settings:
    return Settings()
