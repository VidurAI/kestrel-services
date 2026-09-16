"""Tiny in-memory TTL cache with single-flight loading (used for the carrier list)."""

from __future__ import annotations

import asyncio
import time
from collections import OrderedDict
from collections.abc import Awaitable, Callable
from typing import Generic, Hashable, TypeVar

K = TypeVar("K", bound=Hashable)
V = TypeVar("V")


class TTLCache(Generic[K, V]):
    def __init__(
        self,
        ttl: float,
        *,
        maxsize: int = 128,
        clock: Callable[[], float] = time.monotonic,
    ) -> None:
        self.ttl = ttl
        self.maxsize = maxsize
        self._clock = clock
        self._data: OrderedDict[K, tuple[float, V]] = OrderedDict()
        self._locks: dict[K, asyncio.Lock] = {}
        self.hits = 0
        self.misses = 0

    def __len__(self) -> int:
        return len(self._data)

    def get(self, key: K) -> V | None:
        entry = self._data.get(key)
        if entry is None:
            return None
        expires_at, value = entry
        if self._clock() >= expires_at:
            del self._data[key]
            return None
        self._data.move_to_end(key)
        return value

    def set(self, key: K, value: V) -> None:
        self._data[key] = (self._clock() + self.ttl, value)
        self._data.move_to_end(key)
        while len(self._data) > self.maxsize:
            self._data.popitem(last=False)

    def invalidate(self, key: K | None = None) -> None:
        if key is None:
            self._data.clear()
        else:
            self._data.pop(key, None)

    async def get_or_load(self, key: K, loader: Callable[[], Awaitable[V]]) -> tuple[V, bool]:
        """Return ``(value, was_cached)``; concurrent misses share one ``loader`` call."""
        cached = self.get(key)
        if cached is not None:
            self.hits += 1
            return cached, True
        lock = self._locks.setdefault(key, asyncio.Lock())
        async with lock:
            cached = self.get(key)
            if cached is not None:
                self.hits += 1
                return cached, True
            self.misses += 1
            try:
                value = await loader()
            finally:
                self._locks.pop(key, None)
            self.set(key, value)
            return value, False
