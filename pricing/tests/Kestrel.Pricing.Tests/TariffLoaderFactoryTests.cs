using Kestrel.Pricing.Tariffs;
using Microsoft.Extensions.Configuration;

namespace Kestrel.Pricing.Tests;

public class TariffLoaderFactoryTests
{
    private static IConfiguration Config(string? loader)
    {
        var values = new Dictionary<string, string?> { ["TariffLoader"] = loader };
        return new ConfigurationBuilder().AddInMemoryCollection(values).Build();
    }

    [Fact]
    public void Create_DefaultsToCsvLoaderWhenNothingConfigured()
    {
        var loader = TariffLoaderFactory.CreateFromConfiguration(Config(null));

        Assert.IsType<CsvTariffLoader>(loader);
    }

    [Fact]
    public void Create_ResolvesLoaderByTypeNameFromConfiguration()
    {
        var loader = TariffLoaderFactory.CreateFromConfiguration(
            Config("Kestrel.Pricing.Tariffs.CsvTariffLoader"));

        Assert.IsType<CsvTariffLoader>(loader);
    }

    [Fact]
    public void Create_ThrowsForUnknownType()
    {
        var ex = Assert.Throws<TariffLoadException>(() => TariffLoaderFactory.Create("Kestrel.Pricing.Tariffs.Nope"));

        Assert.Contains("could not be resolved", ex.Message);
    }

    [Fact]
    public void Create_ThrowsForTypeThatIsNotATariffLoader()
    {
        var ex = Assert.Throws<TariffLoadException>(() => TariffLoaderFactory.Create("System.String"));

        Assert.Contains("is not a concrete", ex.Message);
    }

    [Fact]
    public void Create_ThrowsForInterfaceType()
    {
        Assert.Throws<TariffLoadException>(() => TariffLoaderFactory.Create("Kestrel.Pricing.Tariffs.ITariffLoader"));
    }
}
