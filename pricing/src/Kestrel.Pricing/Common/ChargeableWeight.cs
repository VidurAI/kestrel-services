namespace Kestrel.Pricing.Common;

/// <summary>
/// Carriers bill in half-kilogram steps; actual weight is rounded up to the next 0.5 kg.
/// </summary>
public static class ChargeableWeight
{
    public const decimal Step = 0.5m;

    public static decimal Of(decimal actualKg)
    {
        if (actualKg <= 0m)
        {
            throw new ArgumentOutOfRangeException(nameof(actualKg), "weight must be positive");
        }

        return Math.Ceiling(actualKg / Step) * Step;
    }
}
