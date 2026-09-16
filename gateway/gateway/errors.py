"""Gateway error types and the structured error responses built from them.

Every error leaves the gateway as::

    {"detail": "<human readable>", "code": "<machine code>", "request_id": "<id>"}
"""

from __future__ import annotations

import logging
from typing import Any

from fastapi import FastAPI, Request
from fastapi.encoders import jsonable_encoder
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

logger = logging.getLogger("gateway.errors")


class GatewayError(Exception):
    status_code = 500
    code = "internal_error"
    default_detail = "internal server error"

    def __init__(
        self,
        detail: str | None = None,
        *,
        headers: dict[str, str] | None = None,
        extra: dict[str, Any] | None = None,
    ) -> None:
        self.detail = detail or self.default_detail
        self.headers = headers or {}
        self.extra = extra or {}
        super().__init__(self.detail)


class BadRequestError(GatewayError):
    status_code = 422
    code = "invalid_request"
    default_detail = "invalid request"


class NotFoundError(GatewayError):
    status_code = 404
    code = "not_found"
    default_detail = "resource not found"


class ConflictError(GatewayError):
    status_code = 409
    code = "conflict"
    default_detail = "request conflicts with current state"


class UpstreamValidationError(GatewayError):
    """The upstream rejected the payload (HTTP 400/422)."""

    status_code = 422
    code = "upstream_rejected"
    default_detail = "request rejected by upstream"


class RateLimitedError(GatewayError):
    status_code = 429
    code = "rate_limited"
    default_detail = "too many requests"


class UpstreamBadGateway(GatewayError):
    status_code = 502
    code = "bad_gateway"
    default_detail = "unexpected response from upstream"


class UpstreamTimeout(GatewayError):
    status_code = 504
    code = "upstream_timeout"
    default_detail = "upstream timed out"


class UpstreamUnavailable(GatewayError):
    status_code = 503
    code = "upstream_unavailable"
    default_detail = "upstream temporarily unavailable"


class BookingUnavailable(UpstreamUnavailable):
    code = "booking_unavailable"
    default_detail = "booking temporarily unavailable"


class PricingUnavailable(UpstreamUnavailable):
    code = "pricing_unavailable"
    default_detail = "pricing temporarily unavailable"


def _request_id(request: Request) -> str | None:
    return getattr(request.state, "request_id", None)


def error_response(
    request: Request,
    status_code: int,
    detail: Any,
    code: str,
    *,
    headers: dict[str, str] | None = None,
    extra: dict[str, Any] | None = None,
) -> JSONResponse:
    body: dict[str, Any] = {"detail": detail, "code": code, "request_id": _request_id(request)}
    if extra:
        body.update(extra)
    response = JSONResponse(jsonable_encoder(body), status_code=status_code, headers=headers)
    request_id = _request_id(request)
    if request_id:
        response.headers["X-Request-ID"] = request_id
    return response


async def _gateway_error_handler(request: Request, exc: GatewayError) -> JSONResponse:
    if exc.status_code >= 500:
        logger.warning("gateway error", extra={"code": exc.code, "status": exc.status_code})
    return error_response(
        request, exc.status_code, exc.detail, exc.code, headers=exc.headers, extra=exc.extra
    )


async def _http_exception_handler(request: Request, exc: StarletteHTTPException) -> JSONResponse:
    code = "not_found" if exc.status_code == 404 else "http_error"
    return error_response(request, exc.status_code, str(exc.detail), code, headers=exc.headers)


async def _validation_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    errors = [
        {"loc": [str(p) for p in e.get("loc", ())], "msg": e.get("msg", ""), "type": e.get("type", "")}
        for e in exc.errors()
    ]
    return error_response(
        request, 422, "request validation failed", "validation_error", extra={"errors": errors}
    )


async def _unhandled_handler(request: Request, exc: Exception) -> JSONResponse:
    logger.exception("unhandled error", exc_info=exc)
    return error_response(request, 500, "internal server error", "internal_error")


def install_error_handlers(app: FastAPI) -> None:
    app.add_exception_handler(GatewayError, _gateway_error_handler)  # type: ignore[arg-type]
    app.add_exception_handler(StarletteHTTPException, _http_exception_handler)  # type: ignore[arg-type]
    app.add_exception_handler(RequestValidationError, _validation_handler)  # type: ignore[arg-type]
    app.add_exception_handler(Exception, _unhandled_handler)
