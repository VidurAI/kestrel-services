"""Retry with exponential backoff and jitter for upstream calls."""

from __future__ import annotations

import asyncio
import random
from collections.abc import Awaitable, Callable
from dataclasses import dataclass
from typing import TypeVar

import httpx

T = TypeVar("T")

RETRYABLE_STATUSES = frozenset({502, 503, 504})


@dataclass(frozen=True)
class RetryPolicy:
    attempts: int = 3
    base_delay: float = 0.2
    max_delay: float = 2.0
    jitter: float = 0.25  # +/- fraction of the computed delay

    def delay_for(self, attempt: int, rng: Callable[[], float] = random.random) -> float:
        """Delay to sleep after failed attempt number ``attempt`` (1-based)."""
        raw = min(self.max_delay, self.base_delay * (2 ** (attempt - 1)))
        if raw <= 0:
            return 0.0
        spread = raw * self.jitter
        return max(0.0, raw + (rng() * 2 - 1) * spread)


class RetryableStatus(Exception):
    """Raised inside a retried call when the response status warrants another try."""

    def __init__(self, response: httpx.Response) -> None:
        super().__init__(f"retryable status {response.status_code}")
        self.response = response


def is_retryable_error(exc: BaseException, *, idempotent: bool) -> bool:
    """Decide whether ``exc`` may be retried.

    Connection-level failures are always safe: the request never reached the upstream.
    Anything that may have been processed is only retried for idempotent calls.
    """
    if isinstance(exc, (httpx.ConnectError, httpx.ConnectTimeout, httpx.PoolTimeout)):
        return True
    if isinstance(exc, RetryableStatus):
        return idempotent
    if isinstance(exc, httpx.TransportError):
        return idempotent
    return False


async def retry_async(
    fn: Callable[[], Awaitable[T]],
    *,
    policy: RetryPolicy,
    should_retry: Callable[[BaseException], bool],
    sleep: Callable[[float], Awaitable[None]] = asyncio.sleep,
    on_retry: Callable[[int, BaseException, float], None] | None = None,
) -> T:
    """Run ``fn`` up to ``policy.attempts`` times, sleeping between attempts."""
    attempt = 0
    while True:
        attempt += 1
        try:
            return await fn()
        except Exception as exc:  # noqa: BLE001 - classification is delegated
            if attempt >= policy.attempts or not should_retry(exc):
                raise
            delay = policy.delay_for(attempt)
            if on_retry is not None:
                on_retry(attempt, exc, delay)
            await sleep(delay)
