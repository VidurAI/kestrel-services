from __future__ import annotations

from fastapi import APIRouter, Request

from gateway import __version__

router = APIRouter(tags=["health"])


@router.get("/healthz")
async def healthz(request: Request) -> dict[str, object]:
    breakers = {
        name: getattr(request.app.state, name).breaker_state
        for name in ("booking", "pricing")
        if hasattr(getattr(request.app.state, name, None), "breaker_state")
    }
    return {"status": "ok", "version": __version__, "breakers": breakers}
