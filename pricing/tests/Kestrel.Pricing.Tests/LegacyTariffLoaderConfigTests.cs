using Kestrel.Pricing.Tariffs;
using Kestrel.Pricing.Tests.Support;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace Kestrel.Pricing.Tests;

/// <summary>
/// The legacy loader is never referenced from code; it is selected purely by the
/// "TariffLoader" value in appsettings.json. These tests prove that wiring end to end
/// and deliberately refer to the type by name only.
/// </summary>
public class LegacyTariffLoaderConfigTests
{
    private const string LegacyTypeName = "Kestrel.Pricing.Tariffs.LegacyTariffLoader";

    [Fact]
    public void AppSettings_NamesTheLegacyLoader()
    {
        var config = new ConfigurationBuilder().AddJsonFile(RepoPaths.AppSettings()).Build();

        Assert.Equal(LegacyTypeName, config["TariffLoader"]);
    }

    [Fact]
    public void Factory_LoadsLegacyLoaderFromAppSettings()
    {
        var config = new ConfigurationBuilder().AddJsonFile(RepoPaths.AppSettings()).Build();

        var loader = TariffLoaderFactory.CreateFromConfiguration(config);

        Assert.Equal(LegacyTypeName, loader.GetType().FullName);
        Assert.NotEmpty(loader.Load());
    }

    [Fact]
    public void Host_UsesLegacyLoaderConfiguredInAppSettings()
    {
        using var factory = new PricingApiFactory();

        var loader = factory.Services.GetRequiredService<ITariffLoader>();

        Assert.Equal(LegacyTypeName, loader.GetType().FullName);
    }

    [Fact]
    public async Task Healthz_ReportsTheActiveLoader()
    {
        using var factory = new PricingApiFactory();
        var client = factory.CreateClient();

        var body = await client.GetFromJsonAsync<System.Text.Json.JsonElement>("/healthz");

        Assert.Equal(LegacyTypeName, body.GetProperty("tariff_loader").GetString());
        Assert.Equal(36, body.GetProperty("tariff_rows").GetInt32());
    }

    [Fact]
    public void Host_SwitchesToCsvLoaderWhenConfigurationOverridesIt()
    {
        using var factory = new PricingApiFactory();
        factory.ConfigOverrides["TariffLoader"] = "Kestrel.Pricing.Tariffs.CsvTariffLoader";

        var loader = factory.Services.GetRequiredService<ITariffLoader>();

        Assert.IsType<CsvTariffLoader>(loader);
    }
}
