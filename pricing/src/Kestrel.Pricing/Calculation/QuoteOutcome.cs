using Kestrel.Pricing.Common;
using Kestrel.Pricing.Models;

namespace Kestrel.Pricing.Calculation;

/// <summary>Either a quote or an HTTP-mappable error; keeps the service free of ASP.NET types.</summary>
public sealed record QuoteOutcome(int StatusCode, QuoteResponse? Quote, ApiError? Error)
{
    public bool IsSuccess => Quote is not null;

    public static QuoteOutcome Ok(QuoteResponse quote) => new(200, quote, null);

    /// <summary>400: the request is structurally invalid.</summary>
    public static QuoteOutcome Invalid(IReadOnlyList<FieldError> errors)
        => new(400, null, new ApiError(ErrorMessages.InvalidRequest, errors));

    /// <summary>422: the request is well-formed but cannot be priced.</summary>
    public static QuoteOutcome Rejected(string message)
        => new(422, null, new ApiError(message));

    public static QuoteOutcome NotFound() => new(404, null, new ApiError(ErrorMessages.QuoteNotFound));

    public static QuoteOutcome Expired() => new(410, null, new ApiError(ErrorMessages.QuoteExpired));
}
