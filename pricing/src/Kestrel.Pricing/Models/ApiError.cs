namespace Kestrel.Pricing.Models;

/// <summary>Body of every error response: <c>{ "error": "...", "details": [...] }</c>.</summary>
public sealed record ApiError(string Error, IReadOnlyList<FieldError>? Details = null);

public sealed record FieldError(string Field, string Message);
