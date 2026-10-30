# AGENTS.md — booking

Service-level notes for `booking/` (root `AGENTS.md` still applies).

- Build/test here: `mvn -q test` (JUnit 5, H2 under the `test` profile).
- Entry point: `src/main/java/com/kestrel/booking/BookingApplication.java`.
- Wire contract: `POST/GET /v2/shipments`, `POST /v2/shipments/{id}/reserve`, `GET /v2/carriers`;
  response shape is `ShipmentResponse` (snake_case, includes `eta_confidence`). The gateway
  and the web app depend on it.
- Properties live in `src/main/resources/application.yml`; feature flags are under
  `kestrel.booking.features.*`.
- Do not touch: `src/main/resources/db/`.
