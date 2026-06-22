using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace Kestrel.Pricing.Tests.Support;

/// <summary>
/// Hosts the real service in-process. Configuration overrides must be set before the first
/// request; the clock is pinned outside every seasonal surcharge window.
/// </summary>
public sealed class PricingApiFactory : WebApplicationFactory<Program>
{
    public Dictionary<string, string?> ConfigOverrides { get; } = new();

    public FakeTimeProvider Clock { get; } = new(TestDefaults.OffSeason);

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.ConfigureAppConfiguration((_, config) => config.AddInMemoryCollection(ConfigOverrides));
        builder.ConfigureServices(services => services.AddSingleton<TimeProvider>(Clock));
    }
}
