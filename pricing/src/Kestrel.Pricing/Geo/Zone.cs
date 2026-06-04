namespace Kestrel.Pricing.Geo;

/// <summary>Tariff zone for a lane. Carriers price each zone independently.</summary>
public enum Zone
{
    Domestic,
    Regional,
    Intercontinental,
}

public static class ZoneCodes
{
    public static string ToTariffName(Zone zone) => zone switch
    {
        Zone.Domestic => "DOMESTIC",
        Zone.Regional => "REGIONAL",
        Zone.Intercontinental => "INTERCONTINENTAL",
        _ => throw new ArgumentOutOfRangeException(nameof(zone)),
    };

    public static bool TryParse(string? tariffName, out Zone zone)
    {
        switch (tariffName?.Trim().ToUpperInvariant())
        {
            case "DOMESTIC": zone = Zone.Domestic; return true;
            case "REGIONAL": zone = Zone.Regional; return true;
            case "INTERCONTINENTAL": zone = Zone.Intercontinental; return true;
            default: zone = default; return false;
        }
    }
}
