using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Surcharges;

namespace Kestrel.Pricing.Tests;

public class SurchargeEngineTests
{
    private static SurchargeContext Context(string carrier, decimal subtotal, DateOnly? date = null)
        => new(carrier, "NLRTM", "NLAMS", Zone.Domestic, 10m, subtotal, date ?? new DateOnly(2026, 3, 10));

    [Fact]
    public void Evaluate_OnlyRunsRulesOfTheRequestedCarrier()
    {
        var engine = new SurchargeEngine(DefaultSurchargeRules.Create());

        var lines = engine.Evaluate(Context("NSL", 30m));

        Assert.Empty(lines);
        Assert.Empty(engine.Evaluate(Context("NOPE", 30m)));
    }

    [Fact]
    public void Evaluate_LaterRulesSeeEarlierSurchargesInSubtotal()
    {
        var engine = new SurchargeEngine(
        [
            new PeakSeasonRule("T-PEAK", "XXX", (1, 1), (12, 31), 10m, order: 30),
            new MinimumChargeRule("T-MIN", "XXX", 100m, order: 100),
        ]);

        var lines = engine.Evaluate(Context("XXX", 60m));

        Assert.Equal(new[] { "T-PEAK", "T-MIN" }, lines.Select(l => l.Code));
        Assert.Equal(6.00m, lines[0].Amount);
        Assert.Equal(34.00m, lines[1].Amount); // 100 - (60 + 6)
    }

    [Fact]
    public void Evaluate_RunsRulesInOrderRegardlessOfRegistrationOrder()
    {
        var engine = new SurchargeEngine(
        [
            new MinimumChargeRule("T-MIN", "XXX", 100m, order: 100),
            new PeakSeasonRule("T-PEAK", "XXX", (1, 1), (12, 31), 10m, order: 30),
        ]);

        Assert.Equal(new[] { "T-PEAK", "T-MIN" }, engine.Evaluate(Context("XXX", 60m)).Select(l => l.Code));
    }

    [Fact]
    public void Constructor_RejectsDuplicateRuleCodesForACarrier()
    {
        Assert.Throws<InvalidOperationException>(() => new SurchargeEngine(
        [
            new MinimumChargeRule("T-MIN", "XXX", 10m),
            new MinimumChargeRule("T-MIN", "XXX", 20m),
        ]));
    }

    [Fact]
    public void Describe_ListsCarrierRulesInEvaluationOrder()
    {
        var engine = new SurchargeEngine(DefaultSurchargeRules.Create());

        var codes = engine.Describe("PCR").Select(d => d.Code);

        Assert.Equal(new[] { "PCR-REMOTE", "PCR-PEAK", "PCR-MIN" }, codes);
    }

    [Fact]
    public void DefaultRules_CoverAllFourRuleTypes()
    {
        var types = DefaultSurchargeRules.Create().Select(r => r.Describe().Type).Distinct().Order().ToArray();

        Assert.Equal(
            new[] { "minimum_charge", "peak_season", "remote_area", "weight_threshold" },
            types);
    }
}
