# Graph Report - kestrel-services  (2026-10-30)

## Corpus Check
- cluster-only mode — file stats not available

## Summary
- 1582 nodes · 3975 edges · 65 communities (51 shown, 14 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 447 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `cb85a363`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Community 0
- Community 1
- Community 2
- Community 3
- Community 4
- Community 5
- Community 6
- Community 7
- Community 8
- Community 9
- Community 10
- Community 11
- Community 12
- Community 13
- Community 14
- Community 15
- Community 16
- Community 17
- Community 18
- Community 19
- Community 20
- Community 21
- Community 22
- Community 23
- Community 24
- Community 25
- Community 26
- Community 27
- Community 28
- Community 29
- Community 30
- Community 31
- Community 32
- Community 33
- Community 34
- Community 35
- Community 36
- Community 37
- Community 38
- Community 39
- Community 40
- Community 41
- Community 42
- Community 43
- Community 44
- Community 45
- Community 46
- Community 47
- Community 48
- Community 49
- Community 50
- Community 51
- Community 52
- Community 53
- Community 54
- Community 55
- Community 56
- Community 58
- Community 59
- Community 60
- Community 63
- Community 64

## God Nodes (most connected - your core abstractions)
1. `json_response()` - 48 edges
2. `Carrier` - 46 edges
3. `Shipment` - 44 edges
4. `ShipmentStatus` - 32 edges
5. `create_app()` - 28 edges
6. `UpstreamClient` - 26 edges
7. `CarrierCatalog` - 26 edges
8. `BookingClient` - 26 edges
9. `ShipmentControllerTest` - 26 edges
10. `GatewayError` - 24 edges

## Surprising Connections (you probably didn't know these)
- `test_quote_out_breakdown_defaults_to_empty()` --uses--> `QuoteOut`  [INFERRED]
  gateway/tests/test_models.py → gateway/gateway/models.py
- `test_quote_out_uses_decimal_internally_and_numbers_on_the_wire()` --uses--> `QuoteOut`  [INFERRED]
  gateway/tests/test_models.py → gateway/gateway/models.py
- `UpstreamClient` --uses--> `RetryableStatus`  [INFERRED]
  gateway/gateway/clients/base.py → gateway/gateway/retry.py
- `UpstreamClient` --uses--> `RetryPolicy`  [INFERRED]
  gateway/gateway/clients/base.py → gateway/gateway/retry.py
- `BookingClient` --uses--> `GatewayError`  [INFERRED]
  gateway/gateway/clients/booking.py → gateway/gateway/errors.py

## Import Cycles
- None detected.

## Communities (65 total, 14 thin omitted)

### Community 0 - "Community 0"
Cohesion: 0.06
Nodes (27): Kestrel.Pricing.Surcharges, DefaultSurchargeRules, ISurchargeRule, CarrierCode, Code, Order, SurchargeContext, SurchargeDescriptor (+19 more)

### Community 1 - "Community 1"
Cohesion: 0.06
Nodes (8): InMemoryAdvisoryLock, TransactionSynchronization, NoopAdvisoryLock, PostgresAdvisoryLock, AdvisoryKeyTest, InMemoryAdvisoryLockTest, PostgresAdvisoryLockTest, TxHelper

### Community 2 - "Community 2"
Cohesion: 0.08
Nodes (5): CarrierCapacityExhaustedException, CarrierUnavailableException, InvalidShipmentException, ShipmentNotFoundException, ApiExceptionHandler

### Community 3 - "Community 3"
Cohesion: 0.05
Nodes (12): RequestIdFilter, set_request_id(), configure_logging(), JsonFormatter, AccessLogMiddleware, _pick_request_id(), RequestIdMiddleware, test_access_log_line_is_emitted() (+4 more)

### Community 4 - "Community 4"
Cohesion: 0.04
Nodes (48): name, features, lock, ratelimit, password, url, username, health (+40 more)

### Community 5 - "Community 5"
Cohesion: 0.10
Nodes (6): CreateOutcome, EtaEstimator, ShipmentStatusTest, EtaConfidenceScorerTest, EtaEstimatorTest, ShipmentControllerTest

### Community 6 - "Community 6"
Cohesion: 0.11
Nodes (17): PricingClient, get_booking(), get_carrier_directory(), get_pricing(), get_quote_service(), get_settings_dep(), BadRequestError, PricingUnavailable (+9 more)

### Community 7 - "Community 7"
Cohesion: 0.07
Nodes (10): Kestrel.Pricing.Tariffs, Kestrel.Pricing.Tests.Support, Kestrel.Pricing.Tests, Kestrel.Pricing.Geo, HealthEndpoints, HealthResponse, TariffLoaderFactory, LegacyTariffLoaderConfigTests (+2 more)

### Community 8 - "Community 8"
Cohesion: 0.08
Nodes (4): CreateShipmentRequest, DistinctEndpoints, DistinctEndpointsValidator, CreateShipmentRequestValidationTest

### Community 10 - "Community 10"
Cohesion: 0.12
Nodes (12): CircuitBreaker, extract_message(), UpstreamClient, current_request_id(), ConflictError, GatewayError, NotFoundError, RateLimitedError (+4 more)

### Community 11 - "Community 11"
Cohesion: 0.08
Nodes (10): ShipmentStatus, CANCELLED, CREATED, DELIVERED, IN_TRANSIT, RESERVED, ReservationConflictException, CarrierLoad (+2 more)

### Community 12 - "Community 12"
Cohesion: 0.09
Nodes (10): Kestrel.Pricing.Validation, Kestrel.Pricing.Common, Kestrel.Pricing.Calculation, Kestrel.Pricing.Endpoints, Kestrel.Pricing.Models, Kestrel.Pricing.Quotes, Kestrel.Pricing.Options, ErrorMessages (+2 more)

### Community 13 - "Community 13"
Cohesion: 0.12
Nodes (8): CarrierService, CarrierSummary, CarrierController, CarrierMapper, CarrierResponse, ShipmentResponse, ShipmentController, CarrierControllerTest

### Community 14 - "Community 14"
Cohesion: 0.12
Nodes (5): RateLimit, CarrierRateLimiter, TokenBucket, CarrierRateLimiterTest, TokenBucketTest

### Community 15 - "Community 15"
Cohesion: 0.11
Nodes (27): json_response(), test_capacity_exhausted_429_becomes_503_on_create(), test_capacity_exhausted_on_reserve(), test_other_429_is_not_masked(), test_retry_after_is_preserved(), test_missing_carrier_requires_feature_flag(), test_multi_carrier_all_failing_surfaces_first_error(), test_multi_carrier_returns_cheapest() (+19 more)

### Community 16 - "Community 16"
Cohesion: 0.12
Nodes (5): AdvisoryLockWiringTest, Disabled, Enabled, BookingServiceConcurrencyTest, BookingServiceRateLimitTest

### Community 17 - "Community 17"
Cohesion: 0.11
Nodes (8): BookingProperties, Eta, Features, Lock, EtaConfidenceScorer, Locode, ShipmentMapper, ControllerTestConfig

### Community 18 - "Community 18"
Cohesion: 0.17
Nodes (6): CarrierEndpointTests, MultiCarrierQuoteTests, QuoteEndpointTests, PricingApiFactory, Clock, ConfigOverrides

### Community 19 - "Community 19"
Cohesion: 0.13
Nodes (9): CsvTariffLoader, SourceName, EmbeddedData, ITariffLoader, SourceName, TariffLoadException, LegacyTariffLoader, SourceName (+1 more)

### Community 20 - "Community 20"
Cohesion: 0.12
Nodes (15): BreakdownLine, ShipmentIn, ShipmentOut, SurchargeLine, create_shipment(), get_shipment(), reserve_shipment(), test_quote_in_blank_carrier_means_none() (+7 more)

### Community 21 - "Community 21"
Cohesion: 0.11
Nodes (6): ShipmentEvent, Type, CREATED, ETA_REVISED, RESERVE_REJECTED, RESERVED

### Community 22 - "Community 22"
Cohesion: 0.12
Nodes (5): Carrier, CarrierRepository, CarrierCatalog, Snapshot, CarrierCatalogTest

### Community 23 - "Community 23"
Cohesion: 0.11
Nodes (5): QuoteCalculator, FuelSurchargeRoundingTests, QuoteCalculatorTests, FakeTimeProvider, TestDefaults

### Community 24 - "Community 24"
Cohesion: 0.12
Nodes (12): TariffRow, ITariffStore, RowCount, SourceName, Snapshot, TariffStore, RowCount, SourceName (+4 more)

### Community 25 - "Community 25"
Cohesion: 0.12
Nodes (8): Zone, Domestic, Intercontinental, Regional, ZoneCodes, IZoneResolver, ZoneResolver, ZoneResolverTests

### Community 26 - "Community 26"
Cohesion: 0.07
Nodes (28): if, permissions, uses, if, uses, if, uses, jobs (+20 more)

### Community 27 - "Community 27"
Cohesion: 0.17
Nodes (5): UnknownCarrierException, AdvisoryLock, BookingService, CreateShipmentCommand, BookingServiceTest

### Community 28 - "Community 28"
Cohesion: 0.11
Nodes (12): ServiceCollectionExtensions, FeatureOptions, MultiCarrierQuotes, PricingOptions, DefaultCurrency, Features, FuelSurchargePct, MaxQuotesRetained (+4 more)

### Community 29 - "Community 29"
Cohesion: 0.14
Nodes (12): is_retryable_error(), retry_async(), RetryPolicy, booking(), test_delay_grows_exponentially_and_is_capped(), test_jitter_stays_within_bounds(), test_non_retryable_error_raises_immediately(), test_retry_async_gives_up_after_max_attempts() (+4 more)

### Community 30 - "Community 30"
Cohesion: 0.14
Nodes (11): get_settings(), Settings, test_invalid_threshold_rejected_by_settings(), _clear(), test_defaults(), test_flag_parsing(), test_get_settings_is_cached(), test_log_level_is_upper_cased() (+3 more)

### Community 31 - "Community 31"
Cohesion: 0.12
Nodes (11): TTLCache, FakeClock, test_carriers_accepts_wrapped_payload(), test_carriers_listing(), test_get_or_load_is_single_flight(), test_invalidate_one_and_all(), test_second_listing_is_served_from_cache(), test_surcharges_route() (+3 more)

### Community 32 - "Community 32"
Cohesion: 0.14
Nodes (13): create_app(), make_settings(), test_capacity_errors_do_not_trip_the_breaker(), test_breaker_recovers_after_reset_window(), test_gateway_fails_fast_once_booking_breaker_is_open(), test_connection_refused_maps_to_503(), test_malformed_upstream_json_is_502(), test_unexpected_upstream_status_is_502() (+5 more)

### Community 33 - "Community 33"
Cohesion: 0.21
Nodes (6): QuoteOutcome, IsSuccess, QuoteService, QuoteEndpoints, ApiError, QuoteResponse

### Community 35 - "Community 35"
Cohesion: 0.14
Nodes (3): AdvisoryKey, ReferenceGenerator, ReferenceGeneratorTest

### Community 36 - "Community 36"
Cohesion: 0.19
Nodes (7): error_response(), _gateway_error_handler(), _http_exception_handler(), install_error_handlers(), _request_id(), _unhandled_handler(), _validation_handler()

### Community 37 - "Community 37"
Cohesion: 0.23
Nodes (6): InMemoryQuoteRepository, Count, IQuoteRepository, Count, StoredQuote, InMemoryQuoteRepositoryTests

### Community 39 - "Community 39"
Cohesion: 0.19
Nodes (7): Kestrel.Pricing.Carriers, Carrier, ICarrierCatalog, InMemoryCarrierCatalog, CarrierEndpoints, CarrierSummary, CarrierSurchargesResponse

### Community 40 - "Community 40"
Cohesion: 0.14
Nodes (5): _breaker(), lifespan(), _http_client(), healthz(), build_router()

### Community 42 - "Community 42"
Cohesion: 0.17
Nodes (3): BookingConfig, FixedClockConfig, MutableClock

### Community 43 - "Community 43"
Cohesion: 0.12
Nodes (15): features, lock, ratelimit, driver-class-name, password, url, username, eta-confidence (+7 more)

### Community 44 - "Community 44"
Cohesion: 0.22
Nodes (5): BookingClient, BookingUnavailable, test_get_exhausts_retries_and_reports_unavailable(), test_read_timeout_on_get_retries_then_maps_to_504(), test_reserve_is_not_retried_on_503()

### Community 45 - "Community 45"
Cohesion: 0.21
Nodes (3): CalculatedQuote, ChargeableWeight, BreakdownLine

### Community 47 - "Community 47"
Cohesion: 0.47
Nodes (8): BreakerState, Clock, make(), test_failed_probe_reopens_for_a_full_window(), test_half_open_admits_a_single_probe(), test_opens_after_threshold(), test_success_resets_failure_count(), test_successful_probe_closes()

### Community 48 - "Community 48"
Cohesion: 0.38
Nodes (5): FieldError, NormalizedQuoteRequest, QuoteRequestValidator, ValidationResult, IsValid

### Community 49 - "Community 49"
Cohesion: 0.22
Nodes (10): coverlet.collector (6.0.0), Microsoft.AspNetCore.Mvc.Testing (8.0.11), Microsoft.NET.Test.Sdk (17.8.0), xunit (2.5.3), xunit.runner.visualstudio (2.5.3), Kestrel.Pricing, net8.0, Kestrel.Pricing.Tests (+2 more)

### Community 51 - "Community 51"
Cohesion: 0.38
Nodes (4): client(), multi_client(), settings(), upstream()

### Community 52 - "Community 52"
Cohesion: 0.33
Nodes (4): _band(), chargeable_weight(), quote(), _region()

### Community 55 - "Community 55"
Cohesion: 0.33
Nodes (5): description, excluded_tools, prompt, single_project, tool_description_overrides

### Community 58 - "Community 58"
Cohesion: 0.50
Nodes (3): uvx, serena, start-mcp-server

### Community 59 - "Community 59"
Cohesion: 0.50
Nodes (3): github>kestrel-freight/adlc-kit//monitor/renovate-preset.json#v0.1.0, extends, $schema

### Community 60 - "Community 60"
Cohesion: 0.50
Nodes (3): encoding, language_servers, project_name

## Knowledge Gaps
- **58 isolated node(s):** `CarrierCode`, `Code`, `Order`, `RuleType`, `RuleType` (+53 more)
  These have ≤1 connection - possible missing edges. (Counts symbols only; 345 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **14 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `CarrierCatalog` connect `Community 22` to `Community 34`, `Community 5`, `Community 41`, `Community 9`, `Community 11`, `Community 13`, `Community 17`, `Community 27`?**
  _High betweenness centrality (0.036) - this node is a cross-community bridge._
- **What connects `CarrierCode`, `Code`, `Order` to the rest of the system?**
  _58 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Community 0` be split into smaller, more focused modules?**
  _Cohesion score 0.062206572769953054 - nodes in this community are weakly interconnected._
- **Why does `CreateShipmentRequest` connect `Community 8` to `Community 2`, `Community 34`, `Community 9`, `Community 13`, `Community 17`?**
  _High betweenness centrality (0.032) - this node is a cross-community bridge._
- **Should `Community 1` be split into smaller, more focused modules?**
  _Cohesion score 0.06078316773816481 - nodes in this community are weakly interconnected._
- **Why does `CarrierRateLimiter` connect `Community 14` to `Community 11`, `Community 41`, `Community 27`?**
  _High betweenness centrality (0.023) - this node is a cross-community bridge._
- **Should `Community 2` be split into smaller, more focused modules?**
  _Cohesion score 0.07706766917293233 - nodes in this community are weakly interconnected._