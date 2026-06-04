namespace Kestrel.Pricing.Carriers;

/// <summary>
/// Static carrier list. Carrier master data lives in booking; pricing only keeps the handful of
/// attributes it needs to quote (weight ceiling and transit time).
/// </summary>
public sealed class InMemoryCarrierCatalog : ICarrierCatalog
{
    private static readonly Carrier[] Defaults =
    [
        new Carrier("NSL", "NorthStar Lines", Active: true, MaxWeightKg: 20_000, TypicalTransitDays: 4),
        new Carrier("BLX", "Baltic Express", Active: true, MaxWeightKg: 10_000, TypicalTransitDays: 3),
        new Carrier("PCR", "Pacific Reach", Active: true, MaxWeightKg: 20_000, TypicalTransitDays: 12),
    ];

    private readonly Dictionary<string, Carrier> _byCode;

    public InMemoryCarrierCatalog()
        : this(Defaults)
    {
    }

    // Internal so the DI container only sees the parameterless constructor.
    internal InMemoryCarrierCatalog(IEnumerable<Carrier> carriers)
    {
        _byCode = carriers.ToDictionary(c => c.Code, StringComparer.OrdinalIgnoreCase);
    }

    public Carrier? Find(string? code)
    {
        if (string.IsNullOrWhiteSpace(code))
        {
            return null;
        }

        return _byCode.TryGetValue(code.Trim(), out var carrier) ? carrier : null;
    }

    public IReadOnlyList<Carrier> All()
        => _byCode.Values.OrderBy(c => c.Code, StringComparer.Ordinal).ToList();
}
