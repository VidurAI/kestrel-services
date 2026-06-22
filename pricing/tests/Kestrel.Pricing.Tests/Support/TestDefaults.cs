using Kestrel.Pricing.Calculation;
using Kestrel.Pricing.Options;
using Kestrel.Pricing.Surcharges;
using Kestrel.Pricing.Tariffs;
using Microsoft.Extensions.Options;

namespace Kestrel.Pricing.Tests.Support;

/// <summary>Builders for unit tests that need a real calculator without the web host.</summary>
public static class TestDefaults
{
    /// <summary>A date outside every seasonal surcharge window.</summary>
    public static readonly DateTimeOffset OffSeason = new(2026, 3, 10, 12, 0, 0, TimeSpan.Zero);

    public static IOptions<PricingOptions> Options(decimal fuelPct = 8.5m, bool multiCarrier = false)
        => Microsoft.Extensions.Options.Options.Create(new PricingOptions
        {
            FuelSurchargePct = fuelPct,
            Features = new FeatureOptions { MultiCarrierQuotes = multiCarrier },
        });

    public static QuoteCalculator Calculator(decimal fuelPct = 8.5m, DateTimeOffset? now = null)
        => new(
            new TariffStore(new CsvTariffLoader()),
            new SurchargeEngine(DefaultSurchargeRules.Create()),
            Options(fuelPct),
            new FakeTimeProvider(now ?? OffSeason));
}
