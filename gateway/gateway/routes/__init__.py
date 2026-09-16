"""Public route modules, mounted by :func:`gateway.main.create_app`."""

from fastapi import APIRouter

from gateway.routes import carriers, health, quote, shipments


def build_router() -> APIRouter:
    router = APIRouter()
    router.include_router(health.router)
    router.include_router(shipments.router, prefix="/api")
    router.include_router(quote.router, prefix="/api")
    router.include_router(carriers.router, prefix="/api")
    return router
