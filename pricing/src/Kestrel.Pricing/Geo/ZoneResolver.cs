namespace Kestrel.Pricing.Geo;

public interface IZoneResolver
{
    /// <summary>True when <paramref name="locode"/> is a well-formed UN/LOCODE in a served country.</summary>
    bool IsKnownLocation(string? locode);

    /// <summary>Resolves the tariff zone for a lane. Both codes must be known locations.</summary>
    Zone Resolve(string origin, string destination);
}

/// <summary>
/// Derives the zone from the two-letter country prefix of a UN/LOCODE: same country is
/// domestic, same trading region is regional, anything else is intercontinental.
/// </summary>
public sealed class ZoneResolver : IZoneResolver
{
    private static readonly Dictionary<string, string> RegionByCountry = BuildRegions();

    public bool IsKnownLocation(string? locode)
    {
        if (locode is null || locode.Length != 5)
        {
            return false;
        }

        if (!locode.All(char.IsAsciiLetterOrDigit))
        {
            return false;
        }

        return RegionByCountry.ContainsKey(locode[..2].ToUpperInvariant());
    }

    public Zone Resolve(string origin, string destination)
    {
        var from = CountryOf(origin);
        var to = CountryOf(destination);

        if (from == to)
        {
            return Zone.Domestic;
        }

        return RegionByCountry[from] == RegionByCountry[to]
            ? Zone.Regional
            : Zone.Intercontinental;
    }

    private static string CountryOf(string locode)
    {
        var country = locode[..2].ToUpperInvariant();
        if (!RegionByCountry.ContainsKey(country))
        {
            throw new ArgumentException($"unsupported country in location '{locode}'", nameof(locode));
        }

        return country;
    }

    private static Dictionary<string, string> BuildRegions()
    {
        var regions = new Dictionary<string, string[]>
        {
            ["EUROPE"] = ["NL", "DE", "BE", "FR", "ES", "IT", "GB", "PL", "LV", "LT", "EE", "FI", "SE", "DK", "NO", "IS"],
            ["ASIA"] = ["CN", "JP", "KR", "SG", "HK", "VN", "IN"],
            ["NORTH_AMERICA"] = ["US", "CA", "MX"],
            ["OCEANIA"] = ["AU", "NZ"],
        };

        var map = new Dictionary<string, string>(StringComparer.Ordinal);
        foreach (var (region, countries) in regions)
        {
            foreach (var country in countries)
            {
                map[country] = region;
            }
        }

        return map;
    }
}
