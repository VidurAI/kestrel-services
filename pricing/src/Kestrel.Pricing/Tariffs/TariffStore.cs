using Kestrel.Pricing.Geo;

namespace Kestrel.Pricing.Tariffs;

public interface ITariffStore
{
    int RowCount { get; }

    string SourceName { get; }

    /// <summary>Finds the band covering <paramref name="chargeableKg"/>, or null when there is none.</summary>
    TariffRow? Find(string carrierCode, Zone zone, decimal chargeableKg);

    IReadOnlyCollection<string> CarriersWithTariffs();

    /// <summary>Re-reads rows from the loader (used by the tariff reload runbook).</summary>
    void Reload();
}

/// <summary>
/// In-memory index over the rows produced by the configured loader. Reads are lock-free:
/// <see cref="Reload"/> builds a complete new index and swaps it in.
/// </summary>
public sealed class TariffStore : ITariffStore
{
    private readonly ITariffLoader _loader;
    private volatile Snapshot _snapshot;

    public TariffStore(ITariffLoader loader)
    {
        _loader = loader;
        _snapshot = Build(loader.Load());
    }

    public int RowCount => _snapshot.Count;

    public string SourceName => _loader.SourceName;

    public TariffRow? Find(string carrierCode, Zone zone, decimal chargeableKg)
    {
        var key = (carrierCode.ToUpperInvariant(), zone);
        if (!_snapshot.Index.TryGetValue(key, out var bands))
        {
            return null;
        }

        return bands.FirstOrDefault(b => b.Covers(chargeableKg));
    }

    public IReadOnlyCollection<string> CarriersWithTariffs()
        => _snapshot.Carriers;

    public void Reload()
    {
        _snapshot = Build(_loader.Load());
    }

    private static Snapshot Build(IReadOnlyList<TariffRow> rows)
    {
        var index = rows
            .GroupBy(r => (r.CarrierCode, r.Zone))
            .ToDictionary(g => g.Key, g => g.OrderBy(r => r.MinKg).ToArray());

        var carriers = rows.Select(r => r.CarrierCode).Distinct().Order().ToArray();
        return new Snapshot(index, carriers, rows.Count);
    }

    private sealed record Snapshot(
        Dictionary<(string Carrier, Zone Zone), TariffRow[]> Index,
        string[] Carriers,
        int Count);
}
