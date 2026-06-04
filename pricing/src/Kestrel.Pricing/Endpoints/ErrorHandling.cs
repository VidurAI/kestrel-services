using Kestrel.Pricing.Common;
using Kestrel.Pricing.Models;
using Kestrel.Pricing.Tariffs;
using Microsoft.AspNetCore.Diagnostics;

namespace Kestrel.Pricing.Endpoints;

public static class ErrorHandling
{
    /// <summary>
    /// Turns unhandled exceptions into <c>{ "error": "internal error" }</c> with status 500,
    /// and tariff problems into 503 so callers can distinguish "retry later" from "bug".
    /// </summary>
    public static IApplicationBuilder UsePricingErrorHandling(this IApplicationBuilder app)
    {
        return app.UseExceptionHandler(builder => builder.Run(async context =>
        {
            var feature = context.Features.Get<IExceptionHandlerFeature>();
            var logger = context.RequestServices.GetRequiredService<ILoggerFactory>().CreateLogger("Kestrel.Pricing.Errors");

            if (feature?.Error is BadHttpRequestException)
            {
                // Unreadable or missing JSON body: the caller's fault, not ours.
                context.Response.StatusCode = StatusCodes.Status400BadRequest;
                await context.Response.WriteAsJsonAsync(new ApiError(ErrorMessages.InvalidRequest));
                return;
            }

            if (feature?.Error is TariffLoadException)
            {
                logger.LogError(feature.Error, "tariff table unavailable");
                context.Response.StatusCode = StatusCodes.Status503ServiceUnavailable;
            }
            else
            {
                logger.LogError(feature?.Error, "unhandled exception");
                context.Response.StatusCode = StatusCodes.Status500InternalServerError;
            }

            await context.Response.WriteAsJsonAsync(new ApiError(ErrorMessages.Internal));
        }));
    }
}
