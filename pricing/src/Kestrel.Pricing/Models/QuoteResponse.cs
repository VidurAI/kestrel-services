namespace Kestrel.Pricing.Models;

/// <summary>
/// Wire shape of a quote. Property names are snake_cased on the wire
/// (<c>quote_id</c>, <c>valid_until</c>, ...).
/// </summary>
public sealed record QuoteResponse(
    string QuoteId,
    decimal Amount,
    string Currency,
    IReadOnlyList<BreakdownLine> Breakdown,
    string CarrierCode,
    DateTimeOffset ValidUntil);

/// <summary>One priced component of a quote. The lines always sum to <c>amount</c>.</summary>
public sealed record BreakdownLine(string Code, string Description, decimal Amount);
