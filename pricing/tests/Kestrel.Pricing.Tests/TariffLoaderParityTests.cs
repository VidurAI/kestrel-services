using Kestrel.Pricing.Tariffs;

namespace Kestrel.Pricing.Tests;

/// <summary>Both loaders describe the same contract data in different formats.</summary>
public class TariffLoaderParityTests
{
    [Fact]
    public void LegacyAndCsvLoaders_ProduceIdenticalRows()
    {
        var csv = new CsvTariffLoader().Load();
        var legacy = TariffLoaderFactory.Create("Kestrel.Pricing.Tariffs.LegacyTariffLoader").Load();

        Assert.Equal(csv.Count, legacy.Count);
        Assert.Equal(
            csv.OrderBy(Key).ToList(),
            legacy.OrderBy(Key).ToList());
    }

    [Theory]
    [InlineData("Kestrel.Pricing.Tariffs.CsvTariffLoader")]
    [InlineData("Kestrel.Pricing.Tariffs.LegacyTariffLoader")]
    public void EachLoader_CoversEveryCarrierZoneWithContiguousBands(string typeName)
    {
        var rows = TariffLoaderFactory.Create(typeName).Load();
        var groups = rows.GroupBy(r => (r.CarrierCode, r.Zone)).ToList();

        Assert.Equal(9, groups.Count);
        foreach (var group in groups)
        {
            var bands = group.OrderBy(r => r.MinKg).ToList();
            Assert.Equal(0m, bands[0].MinKg);
            for (var i = 1; i < bands.Count; i++)
            {
                Assert.Equal(bands[i - 1].MaxKg, bands[i].MinKg);
            }
        }
    }

    private static string Key(TariffRow row) => $"{row.CarrierCode}/{row.Zone}/{row.MinKg:000000}";
}
