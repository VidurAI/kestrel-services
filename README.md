# kestrel-services

Backend services for Kestrel Freight's booking platform. Polyglot by design — each
service is owned by a different team and built with that team's stack:

| Service | Stack | Owner | Public routes |
|---|---|---|---|
| `booking/` | Java 21 · Spring Boot 3.3 · Maven | @kestrel/booking-team | `/v2/shipments`, `/v2/carriers` |
| `pricing/` | C# · .NET 8 minimal API | @kestrel/pricing-team | `/v1/quote`, `/v1/carriers/{code}/surcharges` |
| `gateway/` | Python 3.12 · FastAPI | @kestrel/platform-team | `/api/*` (public edge) |

The gateway is the only service exposed to the web app; it fans out to booking and
pricing. Decisions and incident history live in the `kestrel-ops` repo.

## Build and test
```
(cd booking && mvn -q test)
(cd pricing && dotnet test)
(cd gateway && uv venv -p 3.12 .venv && uv pip install -p .venv/bin/python -e ".[dev]" && .venv/bin/pytest -q)
```
