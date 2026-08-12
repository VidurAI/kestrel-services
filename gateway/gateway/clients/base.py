"""Shared upstream HTTP plumbing: retries, breaker, and status -> error mapping."""

from __future__ import annotations

import asyncio
import logging
from collections.abc import Awaitable, Callable
from typing import Any, TypeVar

import httpx
from pydantic import BaseModel, ValidationError

from gateway.breaker import CircuitBreaker
from gateway.context import current_request_id
from gateway.errors import (
    ConflictError,
    GatewayError,
    NotFoundError,
    RateLimitedError,
    UpstreamBadGateway,
    UpstreamTimeout,
    UpstreamUnavailable,
    UpstreamValidationError,
)
from gateway.retry import (
    RETRYABLE_STATUSES,
    RetryableStatus,
    RetryPolicy,
    is_retryable_error,
    retry_async,
)

logger = logging.getLogger("gateway.upstream")

M = TypeVar("M", bound=BaseModel)

_SAFE_METHODS = frozenset({"GET", "HEAD", "PUT", "DELETE", "OPTIONS"})
_MESSAGE_KEYS = ("detail", "error", "message", "title")


def extract_message(response: httpx.Response, default: str) -> str:
    """Best-effort human readable message from an upstream error body."""
    try:
        body = response.json()
    except ValueError:
        body = None
    if isinstance(body, dict):
        for key in _MESSAGE_KEYS:
            value = body.get(key)
            if isinstance(value, str) and value:
                return value
    text = response.text.strip()
    return text[:200] if text else default


class UpstreamClient:
    service = "upstream"
    unavailable_error: type[UpstreamUnavailable] = UpstreamUnavailable

    def __init__(
        self,
        http: httpx.AsyncClient,
        *,
        policy: RetryPolicy | None = None,
        breaker: CircuitBreaker | None = None,
        sleep: Callable[[float], Awaitable[None]] = asyncio.sleep,
    ) -> None:
        self._http = http
        self._policy = policy or RetryPolicy()
        self._breaker = breaker
        self._sleep = sleep

    @property
    def breaker_state(self) -> str:
        return self._breaker.state.value if self._breaker is not None else "none"

    # -- hooks ---------------------------------------------------------------

    def translate_status(self, response: httpx.Response) -> GatewayError | None:
        """Subclasses may map service-specific responses before the generic rules."""
        return None

    def counts_as_failure(self, error: GatewayError) -> bool:
        return error.status_code >= 500

    # -- public --------------------------------------------------------------

    async def request(
        self,
        method: str,
        path: str,
        *,
        json: Any = None,
        params: dict[str, Any] | None = None,
        idempotent: bool | None = None,
    ) -> Any:
        method = method.upper()
        if idempotent is None:
            idempotent = method in _SAFE_METHODS
        if self._breaker is not None and not self._breaker.allow():
            raise self.unavailable_error(extra={"reason": "circuit_open"})

        try:
            response = await self._send_with_retries(method, path, json, params, idempotent)
        except GatewayError as exc:
            self._record(exc)
            raise

        error = self._error_for(response)
        if error is not None:
            self._record(error)
            raise error
        self._record(None)
        return self._decode(response)

    def parse(self, model: type[M], data: Any) -> M:
        try:
            return model.model_validate(data)
        except ValidationError as exc:
            logger.error(
                "invalid upstream payload",
                extra={"service": self.service, "model": model.__name__, "errors": exc.error_count()},
            )
            raise UpstreamBadGateway(f"invalid response from {self.service}") from exc

    # -- internals -----------------------------------------------------------

    async def _send_with_retries(
        self, method: str, path: str, body: Any, params: dict[str, Any] | None, idempotent: bool
    ) -> httpx.Response:
        headers = {}
        request_id = current_request_id()
        if request_id:
            headers["X-Request-ID"] = request_id

        async def attempt() -> httpx.Response:
            response = await self._http.request(
                method, path, json=body, params=params, headers=headers
            )
            if response.status_code in RETRYABLE_STATUSES:
                raise RetryableStatus(response)
            return response

        def log_retry(n: int, exc: BaseException, delay: float) -> None:
            logger.warning(
                "retrying upstream call",
                extra={"service": self.service, "path": path, "attempt": n, "delay": round(delay, 3),
                       "cause": type(exc).__name__},
            )

        try:
            return await retry_async(
                attempt,
                policy=self._policy,
                should_retry=lambda exc: is_retryable_error(exc, idempotent=idempotent),
                sleep=self._sleep,
                on_retry=log_retry,
            )
        except RetryableStatus as exc:
            return exc.response
        except httpx.TimeoutException as exc:
            raise UpstreamTimeout(f"{self.service} timed out") from exc
        except httpx.TransportError as exc:
            raise self.unavailable_error() from exc

    def _error_for(self, response: httpx.Response) -> GatewayError | None:
        status = response.status_code
        if status < 400:
            return None
        custom = self.translate_status(response)
        if custom is not None:
            return custom
        message = extract_message(response, f"{self.service} returned {status}")
        if status == 404:
            return NotFoundError(message)
        if status == 409:
            return ConflictError(message)
        if status in (400, 422):
            return UpstreamValidationError(message)
        if status == 429:
            retry_after = response.headers.get("Retry-After")
            headers = {"Retry-After": retry_after} if retry_after else None
            return RateLimitedError(message, headers=headers)
        if status == 503:
            return self.unavailable_error()
        if status == 504:
            return UpstreamTimeout(f"{self.service} timed out")
        return UpstreamBadGateway(f"unexpected response from {self.service}")

    def _decode(self, response: httpx.Response) -> Any:
        if not response.content:
            return None
        try:
            return response.json()
        except ValueError as exc:
            raise UpstreamBadGateway(f"{self.service} returned malformed JSON") from exc

    def _record(self, error: GatewayError | None) -> None:
        if self._breaker is None:
            return
        if error is not None and self.counts_as_failure(error):
            self._breaker.record_failure()
        else:
            self._breaker.record_success()
