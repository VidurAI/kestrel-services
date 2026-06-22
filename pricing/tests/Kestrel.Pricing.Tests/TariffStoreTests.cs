using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Tariffs;

namespace Kestrel.Pricing.Tests;

public class TariffStoreTests
{
    private sealed class CountingLoader : ITariffLoader
    {
        public int Calls { get; private set; }

        public string SourceName => "counting";

        public IReadOnlyList<TariffRow> Load()
        {
            Calls++;
            return [new TariffRow("NSL", Zone.Domestic, 0m, 100m, Calls * 10m, 1m)];
        }
    }

    [Fact]
    public void Find_ReturnsBandCoveringWeight()
    {
        var store = new TariffStore(new CsvTariffLoader());

        var row = store.Find("nsl", Zone.Domestic, 50m);

        Assert.NotNull(row);
        Assert.Equal(50m, row!.MinKg);
        Assert.Equal(250m, row.MaxKg);
    }

    [Fact]
    public void Find_ReturnsNullOutsideKnownBands()
    {
        var store = new TariffStore(new CsvTariffLoader());

        Assert.Null(store.Find("NSL", Zone.Domestic, 100_000m));
        Assert.Null(store.Find("XXX", Zone.Domestic, 10m));
    }

    [Fact]
    public void Reload_SwapsInFreshRows()
    {
        var loader = new CountingLoader();
        var store = new TariffStore(loader);
        Assert.Equal(10m, store.Find("NSL", Zone.Domestic, 5m)!.BaseAmount);

        store.Reload();

        Assert.Equal(2, loader.Calls);
        Assert.Equal(20m, store.Find("NSL", Zone.Domestic, 5m)!.BaseAmount);
    }

    [Fact]
    public void CarriersWithTariffs_ListsDistinctSortedCodes()
    {
        var store = new TariffStore(new CsvTariffLoader());

        Assert.Equal(new[] { "BLX", "NSL", "PCR" }, store.CarriersWithTariffs());
        Assert.Equal(36, store.RowCount);
    }
}
