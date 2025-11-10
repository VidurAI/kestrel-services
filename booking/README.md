# kestrel-booking

Booking service for Kestrel Freight. Owns shipments and carriers, hands out booking
references, and reserves carrier capacity. Owned by `@kestrel/booking-team`.

- Java 21, Spring Boot 3.3.x, Maven (single module), package `com.kestrel.booking`
- Spring Data JPA; PostgreSQL in production, H2 (PostgreSQL mode) in tests
- Consumed by the gateway (`gateway/clients/booking.py`), and through it by the web app

## HTTP API

| Method and path | Success | Notes |
|---|---|---|
| `POST /v2/shipments` | `201` `ShipmentResponse` | Validates the body; `422` for unknown carrier, `429` when the carrier is over its rate limit |
| `GET /v2/shipments/{id}` | `200` `ShipmentResponse` | `404` if the shipment does not exist |
| `POST /v2/shipments/{id}/reserve` | `200` `ShipmentResponse` (status `RESERVED`) | `409` if the shipment cannot be reserved |
| `GET /v2/carriers` | `200` list | Carriers with their open shipment count |
| `GET /v2/carriers/{code}` | `200` | Single carrier |

JSON is snake_case on the wire (Jackson `SNAKE_CASE`). A shipment looks like:

```json
{
  "id": 42,
  "reference": "KB-7Q2M9XK4D8TA",
  "origin": "NLRTM",
  "destination": "DEHAM",
  "carrier_code": "NSL",
  "status": "CREATED",
  "eta": "2026-09-06T10:00:00Z",
  "eta_confidence": 0.87
}
```

`eta_confidence` is a double in 0..1 (BOOK-412, scored by `EtaConfidenceScorer`, see ADR-0008). It
is omitted when the feature flag below is off.

Create request:

```json
{ "origin": "NLRTM", "destination": "DEHAM", "carrier_code": "NSL",
  "weight_kg": 1250.5, "client_reference": "order-9" }
```

`origin`/`destination` are 5-character UN/LOCODEs and must differ. `client_reference` is optional;
repeating a request with the same value returns the original booking (ADR-0006) and sets the
`Idempotent-Replay: true` response header.

Errors are RFC 7807 `application/problem+json`. Every problem body also carries an `error` string.
Rate limit rejections use the string `carrier capacity exhausted` with HTTP 429 and a `Retry-After`
header.

## Configuration

| Key | Default | Meaning |
|---|---|---|
| `kestrel.booking.lock.enabled` | `true` | Selects the real advisory lock implementation; `false` installs a no-op |
| `kestrel.booking.features.eta-confidence` | `true` | Include `eta_confidence` in shipment responses |
| `kestrel.booking.ratelimit.per-carrier-rps` | `20` | Sustained requests per second per carrier (`<= 0` disables) |
| `kestrel.booking.ratelimit.burst-seconds` | `2` | Bucket capacity in seconds of sustained traffic |
| `kestrel.booking.ratelimit.overrides.<CODE>` | none | Per-carrier rps override, e.g. `PCR: 8` |

Database settings come from the environment: `BOOKING_DB_URL`, `BOOKING_DB_USER`,
`BOOKING_DB_PASSWORD`.

## Data

`src/main/resources/db/schema.sql` creates the tables on start and `db/data.sql` seeds the
carriers: NorthStar Lines (`NSL`), Baltic Express (`BLX`) and Pacific Reach (`PCR`). Both scripts
are idempotent.

## Layout

```
config/     properties, clock bean, request-id filter
domain/     JPA entities and the shipment status machine
repo/       Spring Data repositories
service/    BookingService, CarrierService, ETA estimation and scoring, reference generation
lock/       AdvisoryLock and its implementations
ratelimit/  CarrierRateLimiter and TokenBucket
web/        controllers, DTOs, mappers, problem-detail exception handler
```

## Build and test

```
mvn -q test
```

Tests use JUnit 5, Spring Boot test slices and an in-memory H2 database under the `test` profile.
Run locally against Postgres with `mvn spring-boot:run` after exporting the `BOOKING_DB_*` variables.
