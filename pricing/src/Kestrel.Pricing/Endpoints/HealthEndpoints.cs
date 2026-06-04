using Kestrel.Pricing.Tariffs;

namespace Kestrel.Pricing.Endpoints;

public static class HealthEndpoints
{
    public static IEndpointRouteBuilder MapHealthEndpoints(this IEndpointRouteBuilder app)
    {
        app.MapGet("/healthz", (ITariffStore tariffs, ITariffLoader loader) => Results.Ok(new HealthResponse(
            "ok",
            tariffs.RowCount,
            loader.GetType().FullName ?? loader.GetType().Name,
            tariffs.SourceName)));

        return app;
    }
}

public sealed record HealthResponse(string Status, int TariffRows, string TariffLoader, string TariffSource);
