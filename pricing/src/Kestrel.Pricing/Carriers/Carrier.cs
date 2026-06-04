namespace Kestrel.Pricing.Carriers;

public sealed record Carrier(
    string Code,
    string Name,
    bool Active,
    int MaxWeightKg,
    int TypicalTransitDays);
