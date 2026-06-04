using Kestrel.Pricing.Carriers;
using Kestrel.Pricing.Common;
using Kestrel.Pricing.Models;
using Kestrel.Pricing.Options;
using Kestrel.Pricing.Surcharges;
using Microsoft.Extensions.Options;

namespace Kestrel.Pricing.Endpoints;

public static class CarrierEndpoints
{
    public static RouteGroupBuilder MapCarrierEndpoints(this RouteGroupBuilder v1)
    {
        var carriers = v1.MapGroup("/carriers");

        carriers.MapGet("/", ListCarriers)
            .WithName("ListCarriers")
            .Produces<IReadOnlyList<CarrierSummary>>(StatusCodes.Status200OK);

        carriers.MapGet("/{code}/surcharges", GetSurcharges)
            .WithName("GetCarrierSurcharges")
            .Produces<CarrierSurchargesResponse>(StatusCodes.Status200OK)
            .Produces<ApiError>(StatusCodes.Status404NotFound);

        return v1;
    }

    private static IResult ListCarriers(ICarrierCatalog catalog)
        => Results.Ok(catalog.All()
            .Select(c => new CarrierSummary(c.Code, c.Name, c.Active, c.MaxWeightKg, c.TypicalTransitDays))
            .ToList());

    private static IResult GetSurcharges(
        string code,
        ICarrierCatalog catalog,
        SurchargeEngine engine,
        IOptions<PricingOptions> options)
    {
        var carrier = catalog.Find(code);
        if (carrier is null)
        {
            return Results.Json(new ApiError(ErrorMessages.UnknownCarrier), statusCode: StatusCodes.Status404NotFound);
        }

        var body = new CarrierSurchargesResponse(
            carrier.Code,
            options.Value.DefaultCurrency,
            options.Value.FuelSurchargePct,
            engine.Describe(carrier.Code));
        return Results.Ok(body);
    }
}
