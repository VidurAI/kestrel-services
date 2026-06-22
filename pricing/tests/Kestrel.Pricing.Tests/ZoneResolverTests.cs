using Kestrel.Pricing.Geo;

namespace Kestrel.Pricing.Tests;

public class ZoneResolverTests
{
    private readonly ZoneResolver _resolver = new();

    [Theory]
    [InlineData("NLRTM", "NLAMS", Zone.Domestic)]
    [InlineData("NLRTM", "FIHEL", Zone.Regional)]
    [InlineData("nlrtm", "deham", Zone.Regional)]
    [InlineData("NLRTM", "CNSHA", Zone.Intercontinental)]
    [InlineData("USNYC", "CAVAN", Zone.Regional)]
    [InlineData("AUSYD", "NZAKL", Zone.Regional)]
    [InlineData("SGSIN", "USLAX", Zone.Intercontinental)]
    public void Resolve_ClassifiesLanes(string origin, string destination, Zone expected)
    {
        Assert.Equal(expected, _resolver.Resolve(origin, destination));
    }

    [Theory]
    [InlineData("NLRTM", true)]
    [InlineData("nlrtm", true)]
    [InlineData("NLRT", false)]
    [InlineData("NLRTMX", false)]
    [InlineData("N1RTM", false)]
    [InlineData("ZZABC", false)]
    [InlineData("NL RT", false)]
    [InlineData(null, false)]
    public void IsKnownLocation_ChecksFormatAndCountry(string? locode, bool expected)
    {
        Assert.Equal(expected, _resolver.IsKnownLocation(locode));
    }

    [Fact]
    public void Resolve_ThrowsForUnsupportedCountry()
    {
        Assert.Throws<ArgumentException>(() => _resolver.Resolve("ZZABC", "NLRTM"));
    }

    [Theory]
    [InlineData(Zone.Domestic, "DOMESTIC")]
    [InlineData(Zone.Regional, "REGIONAL")]
    [InlineData(Zone.Intercontinental, "INTERCONTINENTAL")]
    public void ZoneCodes_RoundTripTariffNames(Zone zone, string name)
    {
        Assert.Equal(name, ZoneCodes.ToTariffName(zone));
        Assert.True(ZoneCodes.TryParse(name.ToLowerInvariant(), out var parsed));
        Assert.Equal(zone, parsed);
    }
}
