# AGENTS.md — pricing

Service-level notes for `pricing/` (root `AGENTS.md` still applies).

- Build/test here: `dotnet test` from this directory (xunit).
- Entry point: `src/Kestrel.Pricing/Program.cs`; solution `Kestrel.Pricing.sln`.
- Wire contract: `POST /v1/quote` → `quote_id, amount, currency, breakdown[]`;
  `GET /v1/carriers/{code}/surcharges`. The gateway depends on both.
- Configuration: `src/Kestrel.Pricing/appsettings.json` (`Pricing:*`, `TariffLoader`).
  Types named in configuration are loaded by name — grep for the type name in JSON
  before calling anything dead.
- Do not touch: `src/Kestrel.Pricing/Tariffs/Data/`.
