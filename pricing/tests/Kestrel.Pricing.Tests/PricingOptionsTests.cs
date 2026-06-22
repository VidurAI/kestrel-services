using Kestrel.Pricing.Options;

namespace Kestrel.Pricing.Tests;

public class PricingOptionsTests
{
    private readonly PricingOptionsValidator _validator = new();

    [Fact]
    public void Defaults_AreValid()
    {
        Assert.True(_validator.Validate(null, new PricingOptions()).Succeeded);
    }

    [Theory]
    [InlineData(-0.1)]
    [InlineData(100.5)]
    public void FuelSurchargePct_MustBeAPercentage(double pct)
    {
        var result = _validator.Validate(null, new PricingOptions { FuelSurchargePct = (decimal)pct });

        Assert.True(result.Failed);
        Assert.Contains("FuelSurchargePct", result.FailureMessage);
    }

    [Theory]
    [InlineData("")]
    [InlineData("eur")]
    [InlineData("EURO")]
    public void DefaultCurrency_MustBeUpperCaseIsoCode(string currency)
    {
        var result = _validator.Validate(null, new PricingOptions { DefaultCurrency = currency });

        Assert.True(result.Failed);
        Assert.Contains("DefaultCurrency", result.FailureMessage);
    }

    [Fact]
    public void Validate_ReportsAllFailuresTogether()
    {
        var result = _validator.Validate(null, new PricingOptions { QuoteTtlMinutes = 0, MaxQuotesRetained = 0, MaxWeightKg = 0 });

        Assert.Equal(3, result.Failures!.Count());
    }

    [Fact]
    public void MultiCarrierFlag_DefaultsToOff()
    {
        Assert.False(new PricingOptions().Features.MultiCarrierQuotes);
    }
}
