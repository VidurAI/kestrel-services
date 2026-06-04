using Kestrel.Pricing.Models;

namespace Kestrel.Pricing.Quotes;

public sealed record StoredQuote(
    QuoteResponse Quote,
    NormalizedQuoteRequest Request,
    DateTimeOffset CreatedAt);
