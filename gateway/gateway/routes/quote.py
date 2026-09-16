"""``POST /api/quote`` -> pricing ``POST /v1/quote``."""

from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends

from gateway.deps import get_quote_service
from gateway.models import QuoteIn, QuoteOut
from gateway.services import QuoteService

router = APIRouter(tags=["quote"])


@router.post("/quote", response_model=QuoteOut)
async def create_quote(
    payload: QuoteIn, quotes: Annotated[QuoteService, Depends(get_quote_service)]
) -> QuoteOut:
    return await quotes.quote(payload)
