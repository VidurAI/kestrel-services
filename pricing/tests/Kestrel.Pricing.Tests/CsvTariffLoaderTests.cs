using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Tariffs;

namespace Kestrel.Pricing.Tests;

public class CsvTariffLoaderTests
{
    private const string Header = "carrier_code,zone,min_kg,max_kg,base_amount,per_kg";

    private static CsvTariffLoader From(string text) => new(() => new StringReader(text));

    [Fact]
    public void Load_ParsesEmbeddedTable()
    {
        var rows = new CsvTariffLoader().Load();

        Assert.Equal(36, rows.Count);
        Assert.Equal(new[] { "BLX", "NSL", "PCR" }, rows.Select(r => r.CarrierCode).Distinct().Order());
    }

    [Fact]
    public void Load_ParsesSingleRow()
    {
        var rows = From($"{Header}\nnsl,regional,0,50,32.40,0.90\n").Load();

        var row = Assert.Single(rows);
        Assert.Equal("NSL", row.CarrierCode);
        Assert.Equal(Zone.Regional, row.Zone);
        Assert.Equal(32.40m, row.BaseAmount);
        Assert.Equal(0.90m, row.PerKg);
        Assert.True(row.Covers(49.9m));
        Assert.False(row.Covers(50m));
    }

    [Fact]
    public void Load_SkipsCommentsAndBlankLines()
    {
        var rows = From($"{Header}\n\n# note\nBLX,DOMESTIC,0,50,16.56,0.57\n").Load();

        Assert.Single(rows);
    }

    [Theory]
    [InlineData("carrier,zone\nNSL,DOMESTIC,0,50,1,1", "unexpected csv header")]
    [InlineData("{H}\nNSL,DOMESTIC,0,50,1", "expected 6 columns")]
    [InlineData("{H}\nNSL,MOON,0,50,1,1", "unknown zone")]
    [InlineData("{H}\nNSL,DOMESTIC,0,fifty,1,1", "max_kg")]
    [InlineData("{H}\nNSL,DOMESTIC,50,50,1,1", "invalid weight band")]
    [InlineData("{H}\nNSL,DOMESTIC,0,50,-1,1", "base_amount")]
    public void Load_RejectsMalformedInput(string text, string expectedMessage)
    {
        var ex = Assert.Throws<TariffLoadException>(() => From(text.Replace("{H}", Header)).Load());

        Assert.Contains(expectedMessage, ex.Message);
    }

    [Fact]
    public void Load_RejectsEmptyTable()
    {
        Assert.Throws<TariffLoadException>(() => From(Header + "\n").Load());
    }
}
