# adlc_contract — kestrel-services

Derived from AGENTS.md by `adlc baseline`; do not edit here, edit AGENTS.md.

## Build, test, run, lint
- Three independent services; build and test each in its own directory (see the service
  `AGENTS.md` files): `booking/` (Maven), `pricing/` (dotnet), `gateway/` (uv + pytest).
- The oracle for "an edit landed" is that service's test suite. Cross-service edits must
  keep all three green.
- Local run: each service starts on its own port; the gateway reads `BOOKING_BASE_URL` and
  `PRICING_BASE_URL`.

## Conventions
- Wire format is snake_case JSON everywhere (Jackson SNAKE_CASE, System.Text.Json
  SnakeCaseLower, Pydantic aliases). Internal code follows each language's idiom.
- Money is decimal, minor units, rounded AwayFromZero (ADR-0005 in kestrel-ops).
- Errors are RFC 7807 problem details with a stable `detail` string; those strings are
  contract (the gateway matches on them).
- Ticket ids (`BOOK-`, `PRC-`, `GW-`) appear in commit messages and comments; the ticket
  notes live in kestrel-ops, not here.

## Do not touch
- `booking/src/main/resources/db/` — schema and seed are owned by booking + platform; a
  change needs a migration ticket.
- Anything under `pricing/src/Kestrel.Pricing/Tariffs/Data/` — tariff data is loaded by
  configuration; classes there may look unreferenced and still be live.
- `gateway/gateway/legacy/` — scheduled for removal under its own ticket; do not extend.

## Where things are explained
- ADRs, runbooks, postmortems and ticket notes: the kestrel-ops repo (`kestrel-ops:adr/`,
  `kestrel-ops:runbooks/`, `kestrel-ops:postmortems/`, `kestrel-ops:tickets/`). Ownership: `CODEOWNERS`.

<!-- adlc:routing:start -->

## Service: booking
Service-level notes for `booking/` (root `AGENTS.md` still applies).

- Build/test here: `mvn -q test` (JUnit 5, H2 under the `test` profile).
- Entry point: `src/main/java/com/kestrel/booking/BookingApplication.java`.
- Wire contract: `POST/GET /v2/shipments`, `POST /v2/shipments/{id}/reserve`, `GET /v2/carriers`;
  response shape is `ShipmentResponse` (snake_case, includes `eta_confidence`). The gateway
  and the web app depend on it.
- Properties live in `src/main/resources/application.yml`; feature flags are under
  `kestrel.booking.features.*`.
- Do not touch: `src/main/resources/db/`.

## Service: gateway
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

## Service: pricing
Service-level notes for `pricing/` (root `AGENTS.md` still applies).

- Build/test here: `dotnet test` from this directory (xunit).
- Entry point: `src/Kestrel.Pricing/Program.cs`; solution `Kestrel.Pricing.sln`.
- Wire contract: `POST /v1/quote` → `quote_id, amount, currency, breakdown[]`;
  `GET /v1/carriers/{code}/surcharges`. The gateway depends on both.
- Configuration: `src/Kestrel.Pricing/appsettings.json` (`Pricing:*`, `TariffLoader`).
  Types named in configuration are loaded by name — grep for the type name in JSON
  before calling anything dead.
- Do not touch: `src/Kestrel.Pricing/Tariffs/Data/`.

## Context layer
- context: graphify, serena
- artifact: `graphify-out/graph.json` (check `adlc stale`)
- sensitive paths: none declared
