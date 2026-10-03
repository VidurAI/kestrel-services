from __future__ import annotations

import pytest
from pydantic import ValidationError

from gateway.config import Settings, get_settings


def _clear(monkeypatch):
    for key in (
        "BOOKING_BASE_URL",
        "PRICING_BASE_URL",
        "KESTREL_FEATURE_MULTI_CARRIER_QUOTES",
        "GATEWAY_TIMEOUT_SECONDS",
    ):
        monkeypatch.delenv(key, raising=False)


def test_defaults(monkeypatch):
    _clear(monkeypatch)

    s = Settings()

    assert s.booking_base_url == "http://localhost:8081"
    assert s.pricing_base_url == "http://localhost:8082"
    assert s.kestrel_feature_multi_carrier_quotes is False
    assert s.gateway_timeout_seconds == 5.0


def test_reads_environment(monkeypatch):
    _clear(monkeypatch)
    monkeypatch.setenv("BOOKING_BASE_URL", "https://booking.internal/")
    monkeypatch.setenv("PRICING_BASE_URL", "http://pricing.internal:8080")
    monkeypatch.setenv("KESTREL_FEATURE_MULTI_CARRIER_QUOTES", "true")
    monkeypatch.setenv("GATEWAY_TIMEOUT_SECONDS", "2.5")

    s = Settings()

    assert s.booking_base_url == "https://booking.internal"  # trailing slash stripped
    assert s.pricing_base_url == "http://pricing.internal:8080"
    assert s.kestrel_feature_multi_carrier_quotes is True
    assert s.gateway_timeout_seconds == 2.5


@pytest.mark.parametrize("raw,expected", [("1", True), ("0", False), ("TRUE", True), ("no", False)])
def test_flag_parsing(monkeypatch, raw, expected):
    monkeypatch.setenv("KESTREL_FEATURE_MULTI_CARRIER_QUOTES", raw)

    assert Settings().kestrel_feature_multi_carrier_quotes is expected


def test_rejects_non_http_base_url(monkeypatch):
    monkeypatch.setenv("BOOKING_BASE_URL", "booking:8081")

    with pytest.raises(ValidationError):
        Settings()


def test_rejects_non_positive_timeout(monkeypatch):
    monkeypatch.setenv("GATEWAY_TIMEOUT_SECONDS", "0")

    with pytest.raises(ValidationError):
        Settings()


def test_get_settings_is_cached(monkeypatch):
    get_settings.cache_clear()
    try:
        assert get_settings() is get_settings()
    finally:
        get_settings.cache_clear()


def test_log_level_is_upper_cased():
    assert Settings(log_level="debug").log_level == "DEBUG"
