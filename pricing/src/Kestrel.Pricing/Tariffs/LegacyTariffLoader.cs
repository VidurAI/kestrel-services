using System.Globalization;
using Kestrel.Pricing.Common;
using Kestrel.Pricing.Geo;

namespace Kestrel.Pricing.Tariffs;

/// <summary>
/// Reads the embedded <c>tariffs.legacy.txt</c>, the pipe-delimited export produced by the
/// old carrier-contract system: <c>carrier|zone|min_kg|max_kg|base_minor|per_kg_minor</c>
/// where zone is D/R/I and money columns are integer minor units.
/// </summary>
public sealed class LegacyTariffLoader : ITariffLoader
{
    internal const string ResourceName = "tariffs.legacy.txt";
    private const string Magic = "#LEGACY-TARIFF v1";

    private readonly Func<TextReader> _open;

    public LegacyTariffLoader()
        : this(() => EmbeddedData.Open(ResourceName))
    {
    }

    public LegacyTariffLoader(Func<TextReader> open)
    {
        _open = open;
    }

    public string SourceName => "embedded legacy pipe-delimited tariff export";

    public IReadOnlyList<TariffRow> Load()
    {
        using var reader = _open();
        var first = reader.ReadLine();
        if (!string.Equals(first?.Trim(), Magic, StringComparison.Ordinal))
        {
            throw new TariffLoadException("legacy tariff export is missing its version header");
        }

        var rows = new List<TariffRow>();
        var lineNo = 1;
        while (reader.ReadLine() is { } line)
        {
            lineNo++;
            if (string.IsNullOrWhiteSpace(line) || line.TrimStart().StartsWith('#'))
            {
                continue;
            }

            rows.Add(ParseRow(line, lineNo));
        }

        if (rows.Count == 0)
        {
            throw new TariffLoadException("legacy tariff export contains no rows");
        }

        return rows;
    }

    private static TariffRow ParseRow(string line, int lineNo)
    {
        var parts = line.Split('|');
        if (parts.Length != 6)
        {
            throw new TariffLoadException($"line {lineNo}: expected 6 fields but found {parts.Length}");
        }

        var zone = parts[1].Trim() switch
        {
            "D" => Zone.Domestic,
            "R" => Zone.Regional,
            "I" => Zone.Intercontinental,
            var other => throw new TariffLoadException($"line {lineNo}: unknown zone code '{other}'"),
        };

        return new TariffRow(
            parts[0].Trim().ToUpperInvariant(),
            zone,
            Whole(parts[2], lineNo),
            Whole(parts[3], lineNo),
            Money.FromMinorUnits(Whole(parts[4], lineNo)),
            Money.FromMinorUnits(Whole(parts[5], lineNo)));
    }

    private static long Whole(string raw, int lineNo)
    {
        if (!long.TryParse(raw, NumberStyles.None, CultureInfo.InvariantCulture, out var value))
        {
            throw new TariffLoadException($"line {lineNo}: '{raw}' is not a non-negative integer");
        }

        return value;
    }
}
