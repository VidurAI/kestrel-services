# adlc_graph_summary — kestrel-services

Derived from graphify-out/GRAPH_REPORT.md by `adlc baseline`.

## Summary
- 1582 nodes · 3975 edges · 65 communities (51 shown, 14 thin omitted)
- Extraction: 89% EXTRACTED · 11% INFERRED · 0% AMBIGUOUS · INFERRED: 447 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## God nodes (start here for architecture questions)
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

Use `/graphify query|path|explain` for anything structural; `adlc stale` first.
