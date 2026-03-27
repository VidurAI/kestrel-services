# Kestrel Pricing

Quote service for Kestrel Freight. C# on .NET 8 (minimal API), namespace `Kestrel.Pricing`.
Owned by `@kestrel/pricing-team`. Consumed by the gateway (`POST /api/quote` calls
`POST /v1/quote`) and, through the gateway, by the web app.

## Layout

```
Kestrel.Pricing.sln
src/Kestrel.Pricing/        web project
  Calculation/              QuoteService, QuoteCalculator, outcomes
  Carriers/                 carrier catalog (NSL, BLX, PCR)
  Common/                   Money (rounding), chargeable weight, error strings
  Endpoints/                route groups, DI wiring, error handling
  Geo/                      UN/LOCODE -> tariff zone resolution
  Models/                   wire types (snake_case JSON)
  Options/                  PricingOptions + validation
  Quotes/                   in-memory quote repository
  Surcharges/               rules engine (4 rule types) and default schedule
  Tariffs/                  ITariffLoader, CsvTariffLoader, LegacyTariffLoader, factory, store
  Validation/               request validation
tests/Kestrel.Pricing.Tests/ xunit unit and in-process integration tests
```

## HTTP API

| Method | Path | Notes |
|---|---|---|
| POST | `/v1/quote` | body `{ origin, destination, weight_kg, carrier_code }`; 200 `{ quote_id, amount, currency, breakdown[], carrier_code, valid_until }` |
| GET | `/v1/quotes/{quote_id}` | 200, 404 unknown, 410 expired |
| GET | `/v1/carriers` | active carriers |
| GET | `/v1/carriers/{code}/surcharges` | fuel surcharge pct plus the carrier's surcharge rules |
| GET | `/healthz` | liveness plus the active tariff loader |

Errors are `{ "error": "...", "details": [...] }`. An unknown carrier is **422** with
`"unknown carrier code"`; a structurally invalid body is **400** with `"invalid request"`.
JSON is snake_case on the wire (`JsonNamingPolicy.SnakeCaseLower`).

Origin and destination are five-character UN/LOCODEs (`NLRTM`). The zone (domestic,
regional, intercontinental) is derived from the country prefixes.

## Configuration

`src/Kestrel.Pricing/appsettings.json`:

| Key | Meaning |
|---|---|
| `Pricing:FuelSurchargePct` | fuel surcharge percentage applied to the linehaul amount |
| `Pricing:DefaultCurrency` | ISO 4217 currency on every quote |
| `Pricing:Features:MultiCarrierQuotes` | when true, a request without `carrier_code` is priced against every carrier and the cheapest wins |
| `Pricing:QuoteTtlMinutes`, `Pricing:MaxQuotesRetained`, `Pricing:MaxWeightKg` | quote lifetime and limits |
| `TariffLoader` | assembly-qualified-or-local type name of the `ITariffLoader` to use |

### Tariff loaders

`TariffLoaderFactory` turns the `TariffLoader` setting into an `ITariffLoader` with
`Type.GetType` and `Activator.CreateInstance`. Nothing in the code base refers to the
implementations directly, so the classes look unused to a static reference search, but
`appsettings.json` currently selects `Kestrel.Pricing.Tariffs.LegacyTariffLoader` (the
pipe-delimited contract export in minor units). `CsvTariffLoader` reads the same data from
`tariffs.csv` and is the default when the setting is absent. Both data files are embedded
resources under `src/Kestrel.Pricing/Tariffs/Data/`.

## Money and rounding

Amounts are `decimal`. Rounding goes through `Money.Round`, which uses
`MidpointRounding.AwayFromZero` (see ADR-0005, money as decimal minor units).
`QuoteCalculator.ApplyFuelSurcharge` currently calls `Math.Round(value, 2)` directly, which
rounds midpoints to even; the tests in `FuelSurchargeRoundingTests` pin that behaviour down.

## Build and test

```
cd pricing
dotnet test
dotnet run --project src/Kestrel.Pricing
```

Example:

```
curl -s localhost:5000/v1/quote -H 'content-type: application/json' \
  -d '{"origin":"NLRTM","destination":"FIHEL","weight_kg":120,"carrier_code":"NSL"}'
```
