using Kestrel.Pricing.Models;

namespace Kestrel.Pricing.Calculation;

/// <summary>Result of pricing one shipment with one carrier, before it is stored as a quote.</summary>
public sealed record CalculatedQuote(
    string CarrierCode,
    decimal Amount,
    string Currency,
    IReadOnlyList<BreakdownLine> Lines);
