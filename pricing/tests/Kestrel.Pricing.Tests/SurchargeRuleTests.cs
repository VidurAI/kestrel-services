using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Surcharges;

namespace Kestrel.Pricing.Tests;

public class SurchargeRuleTests
{
    private static SurchargeContext Context(
        decimal weight = 10m,
        decimal subtotal = 100m,
        string destination = "NLAMS",
        DateOnly? date = null)
        => new("XXX", "NLRTM", destination, Zone.Domestic, weight, subtotal, date ?? new DateOnly(2026, 3, 10));

    [Fact]
    public void WeightThreshold_ChargesOnlyKilosAboveThreshold()
    {
        var rule = new WeightThresholdRule("T-HEAVY", "XXX", 500m, 0.05m);

        Assert.Null(rule.Evaluate(Context(weight: 500m)));
        Assert.Equal(1.25m, rule.Evaluate(Context(weight: 525m))!.Amount);
    }

    [Fact]
    public void RemoteArea_MatchesCountryOrFullLocode()
    {
        var rule = new RemoteAreaRule("T-REMOTE", "XXX", ["IS", "NOTOS"], 9m);

        Assert.Equal(9m, rule.Evaluate(Context(destination: "ISREK"))!.Amount);
        Assert.Equal(9m, rule.Evaluate(Context(destination: "notos"))!.Amount);
        Assert.Null(rule.Evaluate(Context(destination: "NOOSL")));
    }

    [Theory]
    [InlineData(2026, 10, 14, false)]
    [InlineData(2026, 10, 15, true)]
    [InlineData(2026, 12, 31, true)]
    [InlineData(2027, 1, 5, true)]
    [InlineData(2027, 1, 6, false)]
    [InlineData(2026, 7, 1, false)]
    public void PeakSeason_HandlesWindowWrappingTheYearEnd(int year, int month, int day, bool expected)
    {
        var rule = new PeakSeasonRule("T-PEAK", "XXX", (10, 15), (1, 5), 8m);

        Assert.Equal(expected, rule.IsInSeason(new DateOnly(year, month, day)));
    }

    [Fact]
    public void PeakSeason_AppliesPercentageToRunningSubtotal()
    {
        var rule = new PeakSeasonRule("T-PEAK", "XXX", (11, 1), (12, 31), 12m);

        var line = rule.Evaluate(Context(subtotal: 50m, date: new DateOnly(2026, 11, 2)));

        Assert.Equal(6.00m, line!.Amount);
        Assert.Null(rule.Evaluate(Context(subtotal: 50m)));
    }

    [Fact]
    public void PeakSeason_RoundsWithAwayFromZero()
    {
        var rule = new PeakSeasonRule("T-PEAK", "XXX", (1, 1), (12, 31), 12.5m);

        // 12.5% of 0.84 is 0.105: Money.Round takes it to 0.11.
        Assert.Equal(0.11m, rule.Evaluate(Context(subtotal: 0.84m))!.Amount);
    }

    [Fact]
    public void MinimumCharge_TopsUpOnlyTheShortfall()
    {
        var rule = new MinimumChargeRule("T-MIN", "XXX", 75m);

        Assert.Equal(25.50m, rule.Evaluate(Context(subtotal: 49.50m))!.Amount);
        Assert.Null(rule.Evaluate(Context(subtotal: 75m)));
        Assert.Null(rule.Evaluate(Context(subtotal: 80m)));
    }

    [Fact]
    public void Describe_ExposesTypeAndSnakeCaseParameters()
    {
        var descriptor = new WeightThresholdRule("T-HEAVY", "XXX", 500m, 0.05m).Describe();

        Assert.Equal(SurchargeTypes.WeightThreshold, descriptor.Type);
        Assert.Equal("500", descriptor.Parameters["threshold_kg"]);
        Assert.Equal("0.05", descriptor.Parameters["per_kg_over"]);
    }
}
