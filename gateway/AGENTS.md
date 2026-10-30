# AGENTS.md — gateway

Service-level notes for `gateway/` (root `AGENTS.md` still applies).

- Build/test here: `uv venv -p 3.12 .venv && uv pip install -p .venv/bin/python -e ".[dev]"`,
  then `.venv/bin/pytest -q` (respx mocks; no live upstreams needed).
- Entry point: `gateway/main.py` (FastAPI app factory).
- Wire contract: public `/api/*` routes; upstream clients in `gateway/clients/` call
  booking `/v2/*` and pricing `/v1/*`. Response models in `gateway/models.py` are what
  the web app sees.
- Settings via environment (`gateway/settings.py`): `BOOKING_BASE_URL`, `PRICING_BASE_URL`,
  `GATEWAY_TIMEOUT_SECONDS`, `KESTREL_FEATURE_MULTI_CARRIER_QUOTES`.
- Do not touch: `gateway/legacy/`.
