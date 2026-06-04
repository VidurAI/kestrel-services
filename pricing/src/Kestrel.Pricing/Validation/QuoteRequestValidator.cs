using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Models;
using Kestrel.Pricing.Options;
using Microsoft.Extensions.Options;

namespace Kestrel.Pricing.Validation;

public sealed record ValidationResult(NormalizedQuoteRequest? Value, IReadOnlyList<FieldError> Errors)
{
    public bool IsValid => Errors.Count == 0 && Value is not null;
}

/// <summary>
/// Structural validation of a <see cref="QuoteRequest"/>. Business checks (unknown carrier,
/// missing tariff, carrier weight limit) happen later in the quote service so that they can
/// answer 422 instead of 400.
/// </summary>
public sealed class QuoteRequestValidator
{
    private readonly IZoneResolver _zones;
    private readonly IOptions<PricingOptions> _options;

    public QuoteRequestValidator(IZoneResolver zones, IOptions<PricingOptions> options)
    {
        _zones = zones;
        _options = options;
    }

    public ValidationResult Validate(QuoteRequest? request)
    {
        if (request is null)
        {
            return new ValidationResult(null, [new FieldError("body", "request body is required")]);
        }

        var errors = new List<FieldError>();
        var origin = CheckLocation("origin", request.Origin, errors);
        var destination = CheckLocation("destination", request.Destination, errors);
        var weight = CheckWeight(request.WeightKg, errors);
        var carrier = CheckCarrier(request.CarrierCode, errors);

        if (errors.Count > 0)
        {
            return new ValidationResult(null, errors);
        }

        return new ValidationResult(
            new NormalizedQuoteRequest(origin!, destination!, weight!.Value, carrier),
            errors);
    }

    private string? CheckLocation(string field, string? raw, List<FieldError> errors)
    {
        if (string.IsNullOrWhiteSpace(raw))
        {
            errors.Add(new FieldError(field, $"{field} is required"));
            return null;
        }

        var code = raw.Trim().ToUpperInvariant();
        if (!_zones.IsKnownLocation(code))
        {
            errors.Add(new FieldError(field, $"{field} must be a five-character UN/LOCODE in a served country"));
            return null;
        }

        return code;
    }

    private decimal? CheckWeight(decimal? weight, List<FieldError> errors)
    {
        if (weight is null)
        {
            errors.Add(new FieldError("weight_kg", "weight_kg is required"));
            return null;
        }

        if (weight <= 0m)
        {
            errors.Add(new FieldError("weight_kg", "weight_kg must be greater than zero"));
            return null;
        }

        if (weight > _options.Value.MaxWeightKg)
        {
            errors.Add(new FieldError("weight_kg", $"weight_kg must not exceed {_options.Value.MaxWeightKg:0.##}"));
            return null;
        }

        return weight;
    }

    private string? CheckCarrier(string? raw, List<FieldError> errors)
    {
        if (!string.IsNullOrWhiteSpace(raw))
        {
            return raw.Trim().ToUpperInvariant();
        }

        if (!_options.Value.Features.MultiCarrierQuotes)
        {
            errors.Add(new FieldError("carrier_code", "carrier_code is required"));
        }

        return null;
    }
}
