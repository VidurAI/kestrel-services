using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Models;
using Kestrel.Pricing.Tests.Support;
using Kestrel.Pricing.Validation;

namespace Kestrel.Pricing.Tests;

public class QuoteRequestValidatorTests
{
    private static QuoteRequestValidator Validator(bool multiCarrier = false)
        => new(new ZoneResolver(), TestDefaults.Options(multiCarrier: multiCarrier));

    private static QuoteRequest Valid() => new("NLRTM", "FIHEL", 12.5m, "NSL");

    [Fact]
    public void Validate_AcceptsAndNormalisesAValidRequest()
    {
        var result = Validator().Validate(new QuoteRequest(" nlrtm ", "fihel", 12.5m, " nsl "));

        Assert.True(result.IsValid);
        Assert.Equal(new NormalizedQuoteRequest("NLRTM", "FIHEL", 12.5m, "NSL"), result.Value);
    }

    [Fact]
    public void Validate_RejectsNullBody()
    {
        var result = Validator().Validate(null);

        Assert.False(result.IsValid);
        Assert.Equal("body", result.Errors.Single().Field);
    }

    [Fact]
    public void Validate_ReportsEveryMissingField()
    {
        var result = Validator().Validate(new QuoteRequest(null, null, null, null));

        Assert.Equal(
            new[] { "origin", "destination", "weight_kg", "carrier_code" },
            result.Errors.Select(e => e.Field));
    }

    [Theory]
    [InlineData(0)]
    [InlineData(-3)]
    [InlineData(20_001)]
    public void Validate_RejectsOutOfRangeWeight(int weight)
    {
        var result = Validator().Validate(Valid() with { WeightKg = weight });

        Assert.Equal("weight_kg", Assert.Single(result.Errors).Field);
    }

    [Theory]
    [InlineData("ROTTERDAM")]
    [InlineData("ZZABC")]
    [InlineData("12345")]
    public void Validate_RejectsMalformedOrUnservedLocations(string origin)
    {
        var result = Validator().Validate(Valid() with { Origin = origin });

        Assert.Equal("origin", Assert.Single(result.Errors).Field);
    }

    [Fact]
    public void Validate_AllowsMissingCarrierWhenMultiCarrierFlagIsOn()
    {
        var result = Validator(multiCarrier: true).Validate(Valid() with { CarrierCode = null });

        Assert.True(result.IsValid);
        Assert.Null(result.Value!.CarrierCode);
    }

    [Fact]
    public void Validate_DoesNotCheckCarrierCodeFormat()
    {
        // Unknown carriers are a business error (422), not a validation error (400).
        var result = Validator().Validate(Valid() with { CarrierCode = "???" });

        Assert.True(result.IsValid);
    }
}
