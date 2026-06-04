namespace Kestrel.Pricing.Models;

/// <summary>
/// Wire shape of <c>POST /v1/quote</c>. Every member is nullable so that missing fields
/// surface as validation errors instead of model-binding failures.
/// </summary>
public sealed record QuoteRequest(
    string? Origin,
    string? Destination,
    decimal? WeightKg,
    string? CarrierCode);

/// <summary>A request after validation and normalisation (upper-case codes, weight present).</summary>
public sealed record NormalizedQuoteRequest(
    string Origin,
    string Destination,
    decimal WeightKg,
    string? CarrierCode);
