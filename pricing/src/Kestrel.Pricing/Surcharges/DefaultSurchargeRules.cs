namespace Kestrel.Pricing.Surcharges;

/// <summary>
/// The surcharge schedule agreed in the current carrier contracts. Fuel surcharge is not
/// listed here: it is a percentage from configuration applied by the quote calculator.
/// </summary>
public static class DefaultSurchargeRules
{
    public static IReadOnlyList<ISurchargeRule> Create() =>
    [
        // NorthStar Lines
        new WeightThresholdRule("NSL-HEAVY", "NSL", thresholdKg: 500m, perKgOver: 0.05m),
        new RemoteAreaRule("NSL-REMOTE", "NSL", ["FI", "IS"], flatAmount: 12.50m),

        // Baltic Express
        new WeightThresholdRule("BLX-HEAVY", "BLX", thresholdKg: 1000m, perKgOver: 0.04m),
        new RemoteAreaRule("BLX-REMOTE", "BLX", ["NO", "IS"], flatAmount: 9.00m),
        new PeakSeasonRule("BLX-PEAK", "BLX", start: (10, 15), end: (1, 5), pct: 8m),

        // Pacific Reach
        new RemoteAreaRule("PCR-REMOTE", "PCR", ["NZ"], flatAmount: 35.00m),
        new PeakSeasonRule("PCR-PEAK", "PCR", start: (11, 1), end: (12, 31), pct: 12m),
        new MinimumChargeRule("PCR-MIN", "PCR", minimum: 75.00m),
    ];
}
