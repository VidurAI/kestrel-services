using System.Text.Json;
using System.Text.Json.Serialization;
using Kestrel.Pricing.Calculation;
using Kestrel.Pricing.Carriers;
using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Options;
using Kestrel.Pricing.Quotes;
using Kestrel.Pricing.Surcharges;
using Kestrel.Pricing.Tariffs;
using Kestrel.Pricing.Validation;
using Microsoft.AspNetCore.Http.Json;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.Extensions.Options;

namespace Kestrel.Pricing.Endpoints;

public static class ServiceCollectionExtensions
{
    public static IServiceCollection AddPricing(this IServiceCollection services, IConfiguration configuration)
    {
        services.AddOptions<PricingOptions>()
            .Bind(configuration.GetSection(PricingOptions.SectionName))
            .ValidateOnStart();
        services.TryAddEnumerable(ServiceDescriptor.Singleton<IValidateOptions<PricingOptions>, PricingOptionsValidator>());

        services.Configure<JsonOptions>(o =>
        {
            o.SerializerOptions.PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower;
            o.SerializerOptions.DictionaryKeyPolicy = JsonNamingPolicy.SnakeCaseLower;
            o.SerializerOptions.DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull;
        });

        services.TryAddSingleton(TimeProvider.System);
        services.AddSingleton<ICarrierCatalog, InMemoryCarrierCatalog>();
        services.AddSingleton<IZoneResolver, ZoneResolver>();

        // The loader implementation is picked by name from the "TariffLoader" setting.
        // Resolved lazily so test hosts can override configuration before first use.
        services.AddSingleton<ITariffLoader>(sp =>
            TariffLoaderFactory.CreateFromConfiguration(sp.GetRequiredService<IConfiguration>()));
        services.AddSingleton<ITariffStore, TariffStore>();

        foreach (var rule in DefaultSurchargeRules.Create())
        {
            services.AddSingleton<ISurchargeRule>(rule);
        }

        services.AddSingleton<SurchargeEngine>();
        services.AddSingleton<QuoteCalculator>();
        services.AddSingleton<QuoteRequestValidator>();
        services.AddSingleton<IQuoteRepository, InMemoryQuoteRepository>();
        services.AddSingleton<QuoteService>();
        return services;
    }

    public static WebApplication MapPricingEndpoints(this WebApplication app)
    {
        app.UsePricingErrorHandling();
        app.MapHealthEndpoints();

        var v1 = app.MapGroup("/v1");
        v1.MapQuoteEndpoints();
        v1.MapCarrierEndpoints();
        return app;
    }
}
