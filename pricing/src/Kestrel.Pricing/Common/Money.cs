namespace Kestrel.Pricing.Common;

/// <summary>
/// Monetary arithmetic helpers. All amounts in the pricing service are <see cref="decimal"/>.
/// </summary>
public static class Money
{
    /// <summary>Number of fractional digits for every supported currency (EUR, GBP, USD).</summary>
    public const int Scale = 2;

    // ADR-0005 (money-as-decimal-minor-units): amounts are decimal, never double, and a
    // midpoint always rounds away from zero so 0.125 becomes 0.13, matching carrier invoices
    // and the finance ledger. Any rounding of an amount in this service goes through here.
    public static decimal Round(decimal value, int decimals = Scale)
        => Math.Round(value, decimals, MidpointRounding.AwayFromZero);

    /// <summary>Converts a major-unit amount (12.34) to minor units (1234).</summary>
    public static long ToMinorUnits(decimal amount)
        => (long)Round(amount * 100m, 0);

    /// <summary>Converts minor units (1234) back to a major-unit amount (12.34).</summary>
    public static decimal FromMinorUnits(long minor)
        => minor / 100m;

    /// <summary>Sums amounts and normalises the result to <see cref="Scale"/> digits.</summary>
    public static decimal Sum(IEnumerable<decimal> amounts)
        => Round(amounts.Sum());

    /// <summary><paramref name="pct"/> percent of <paramref name="baseAmount"/>, rounded.</summary>
    public static decimal Percent(decimal baseAmount, decimal pct)
        => Round(baseAmount * pct / 100m);

    /// <summary>True when the amount has no more than <see cref="Scale"/> fractional digits.</summary>
    public static bool IsNormalised(decimal amount)
        => amount == Round(amount);
}
