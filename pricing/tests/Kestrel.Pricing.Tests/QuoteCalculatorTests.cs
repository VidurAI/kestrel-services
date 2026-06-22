using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Tests.Support;

namespace Kestrel.Pricing.Tests;

public class QuoteCalculatorTests
{
    private static readonly DateTimeOffset PeakDay = new(2026, 11, 20, 9, 0, 0, TimeSpan.Zero);

    [Fact]
    public void Calculate_DomesticNsl_SumsBaseWeightAndFuel()
    {
        var quote = TestDefaults.Calculator().Calculate("NSL", "NLRTM", "NLAMS", Zone.Domestic, 10m);

        Assert.NotNull(quote);
        Assert.Equal(new[] { "BASE", "WEIGHT", "FUEL" }, quote!.Lines.Select(l => l.Code));
        Assert.Equal(18.00m, quote.Lines[0].Amount);
        Assert.Equal(6.00m, quote.Lines[1].Amount);
        Assert.Equal(2.04m, quote.Lines[2].Amount);
        Assert.Equal(26.04m, quote.Amount);
        Assert.Equal("EUR", quote.Currency);
    }

    [Fact]
    public void Calculate_BreakdownAlwaysSumsToAmount()
    {
        var calculator = TestDefaults.Calculator(now: PeakDay);

        foreach (var carrier in new[] { "NSL", "BLX", "PCR" })
        {
            var quote = calculator.Calculate(carrier, "NLRTM", "FIHEL", Zone.Regional, 123.3m);

            Assert.NotNull(quote);
            Assert.Equal(quote!.Amount, quote.Lines.Sum(l => l.Amount));
        }
    }

    [Fact]
    public void Calculate_UsesChargeableWeightForBandAndPrice()
    {
        // 49.8 kg bills as 50.0 kg, which falls into the 50-250 band (30.00 + 0.45/kg).
        var quote = TestDefaults.Calculator().Calculate("NSL", "NLRTM", "NLAMS", Zone.Domestic, 49.8m);

        Assert.NotNull(quote);
        Assert.Equal(30.00m, quote!.Lines[0].Amount);
        Assert.Equal(22.50m, quote.Lines[1].Amount);
    }

    [Fact]
    public void Calculate_AddsHeavySurchargeAboveThreshold()
    {
        var quote = TestDefaults.Calculator().Calculate("NSL", "NLRTM", "NLAMS", Zone.Domestic, 600m);

        Assert.NotNull(quote);
        var heavy = quote!.Lines.Single(l => l.Code == "NSL-HEAVY");
        Assert.Equal(5.00m, heavy.Amount); // (600 - 500) * 0.05
    }

    [Fact]
    public void Calculate_PcrMinimumChargeTopsUpSmallShipments()
    {
        var quote = TestDefaults.Calculator().Calculate("PCR", "NLRTM", "NLAMS", Zone.Domestic, 2m);

        Assert.NotNull(quote);
        Assert.Contains(quote!.Lines, l => l.Code == "PCR-MIN");
        Assert.Equal(75.00m, quote.Amount);
    }

    [Fact]
    public void Calculate_PeakSeasonUpliftsOnlyInsideWindow()
    {
        var off = TestDefaults.Calculator().Calculate("BLX", "NLRTM", "NLAMS", Zone.Domestic, 10m);
        var peak = TestDefaults.Calculator(now: PeakDay).Calculate("BLX", "NLRTM", "NLAMS", Zone.Domestic, 10m);

        Assert.DoesNotContain(off!.Lines, l => l.Code == "BLX-PEAK");
        Assert.Contains(peak!.Lines, l => l.Code == "BLX-PEAK");
        Assert.True(peak.Amount > off.Amount);
    }

    [Fact]
    public void Calculate_ReturnsNullWhenNoBandCoversWeight()
    {
        var quote = TestDefaults.Calculator().Calculate("NSL", "NLRTM", "NLAMS", Zone.Domestic, 150_000m);

        Assert.Null(quote);
    }

    [Fact]
    public void Calculate_ReturnsNullForCarrierWithoutTariff()
    {
        Assert.Null(TestDefaults.Calculator().Calculate("ZZZ", "NLRTM", "NLAMS", Zone.Domestic, 10m));
    }
}
