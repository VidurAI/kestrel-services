using System.Globalization;
using Kestrel.Pricing.Geo;

namespace Kestrel.Pricing.Tariffs;

/// <summary>
/// Reads the embedded <c>tariffs.csv</c>:
/// <c>carrier_code,zone,min_kg,max_kg,base_amount,per_kg</c> with major-unit decimals.
/// </summary>
public sealed class CsvTariffLoader : ITariffLoader
{
    internal const string ResourceName = "tariffs.csv";
    private const string Header = "carrier_code,zone,min_kg,max_kg,base_amount,per_kg";

    private readonly Func<TextReader> _open;

    public CsvTariffLoader()
        : this(() => EmbeddedData.Open(ResourceName))
    {
    }

    public CsvTariffLoader(Func<TextReader> open)
    {
        _open = open;
    }

    public string SourceName => "embedded csv tariff table";

    public IReadOnlyList<TariffRow> Load()
    {
        using var reader = _open();
        var rows = new List<TariffRow>();
        var lineNo = 0;

        while (reader.ReadLine() is { } line)
        {
            lineNo++;
            if (string.IsNullOrWhiteSpace(line) || line.TrimStart().StartsWith('#'))
            {
                continue;
            }

            if (lineNo == 1)
            {
                RequireHeader(line);
                continue;
            }

            rows.Add(ParseRow(line, lineNo));
        }

        if (rows.Count == 0)
        {
            throw new TariffLoadException("csv tariff table contains no rows");
        }

        return rows;
    }

    private static void RequireHeader(string line)
    {
        if (!string.Equals(line.Trim(), Header, StringComparison.Ordinal))
        {
            throw new TariffLoadException($"unexpected csv header '{line.Trim()}'");
        }
    }

    private static TariffRow ParseRow(string line, int lineNo)
    {
        var parts = line.Split(',');
        if (parts.Length != 6)
        {
            throw new TariffLoadException($"line {lineNo}: expected 6 columns but found {parts.Length}");
        }

        if (!ZoneCodes.TryParse(parts[1], out var zone))
        {
            throw new TariffLoadException($"line {lineNo}: unknown zone '{parts[1]}'");
        }

        var row = new TariffRow(
            parts[0].Trim().ToUpperInvariant(),
            zone,
            Number(parts[2], "min_kg", lineNo),
            Number(parts[3], "max_kg", lineNo),
            Number(parts[4], "base_amount", lineNo),
            Number(parts[5], "per_kg", lineNo));

        if (row.MinKg < 0m || row.MaxKg <= row.MinKg)
        {
            throw new TariffLoadException($"line {lineNo}: invalid weight band {row.MinKg}-{row.MaxKg}");
        }

        return row;
    }

    private static decimal Number(string raw, string column, int lineNo)
    {
        if (!decimal.TryParse(raw, NumberStyles.AllowDecimalPoint, CultureInfo.InvariantCulture, out var value)
            || value < 0m)
        {
            throw new TariffLoadException($"line {lineNo}: column {column} is not a non-negative number: '{raw}'");
        }

        return value;
    }
}
