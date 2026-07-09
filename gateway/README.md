# kestrel-gateway

Public API gateway for Kestrel Freight. It fronts the booking service (Java,
`/v2`) and the pricing service (.NET, `/v1`) and is the only backend the web
app talks to. Owned by `@kestrel/platform-team`.

Python 3.12, FastAPI, httpx, pydantic v2, pydantic-settings.

## Routes

| Gateway route | Upstream |
|---|---|
| `POST /api/shipments` | booking `POST /v2/shipments` |
| `GET /api/shipments/{id}` | booking `GET /v2/shipments/{id}` |
| `POST /api/shipments/{id}/reserve` | booking `POST /v2/shipments/{id}/reserve` |
| `GET /api/carriers` | booking `GET /v2/carriers` (cached, see below) |
| `GET /api/carriers/{code}/surcharges` | pricing `GET /v1/carriers/{code}/surcharges` |
| `POST /api/quote` | pricing `POST /v1/quote` |
| `GET /healthz` | local; reports circuit breaker state |

All JSON is snake_case. Errors always look like
`{"detail": "...", "code": "...", "request_id": "..."}`.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `BOOKING_BASE_URL` | `http://localhost:8081` | booking service root |
| `PRICING_BASE_URL` | `http://localhost:8082` | pricing service root |
| `KESTREL_FEATURE_MULTI_CARRIER_QUOTES` | `false` | allow `/api/quote` without `carrier_code`; quotes every active carrier and returns the cheapest |
| `GATEWAY_TIMEOUT_SECONDS` | `5` | per upstream call timeout |
| `GATEWAY_RETRY_ATTEMPTS` | `3` | attempts per upstream call |
| `GATEWAY_CARRIERS_CACHE_TTL_SECONDS` | `60` | TTL of the in-memory carrier list |
| `GATEWAY_BREAKER_FAILURE_THRESHOLD` / `GATEWAY_BREAKER_RESET_SECONDS` | `5` / `15` | per-upstream circuit breaker |
| `LOG_LEVEL` | `INFO` | JSON logs on stdout |

## Behaviour worth knowing

- **Capacity errors.** When booking answers `429` with a body containing
  `carrier capacity exhausted`, the gateway returns `503` with detail
  `booking temporarily unavailable` (and keeps `Retry-After`). Throttled calls
  are never retried and do not count against the circuit breaker. See the
  `gateway-429-storm` runbook in kestrel-ops.
- **Retries.** Exponential backoff with jitter on connection errors and
  502/503/504. Non-idempotent calls (reserve, create without a `reference`)
  are only retried when the request provably never reached the upstream.
- **Request ids.** `X-Request-ID` is accepted (or minted), echoed on every
  response, attached to every log line, and forwarded to booking and pricing.
- **Carrier cache.** `GET /api/carriers` is cached in-process; the
  `X-Cache: HIT|MISS` header shows which. Concurrent misses share one upstream call.

## Development

```sh
uv venv -p 3.12 .venv
uv pip install -p .venv/bin/python -e ".[dev]"
.venv/bin/pytest -q
.venv/bin/uvicorn gateway.main:app --reload --port 8080
```

## Layout

```
gateway/
  main.py            app factory + lifespan (builds clients, wires services)
  config.py          pydantic-settings Settings
  models.py          public request/response models
  errors.py          error types + structured handlers
  middleware.py      request-id and access-log middleware (pure ASGI)
  retry.py breaker.py cache.py   resilience helpers
  services.py        carrier directory, quote orchestration
  clients/           booking.py, pricing.py on top of base.py
  routes/            shipments, quote, carriers, health
  legacy/            old local pricing code
tests/               respx-based tests, one module per concern
```
