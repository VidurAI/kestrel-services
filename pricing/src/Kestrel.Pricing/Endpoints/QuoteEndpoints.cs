using Kestrel.Pricing.Calculation;
using Kestrel.Pricing.Models;

namespace Kestrel.Pricing.Endpoints;

public static class QuoteEndpoints
{
    public static RouteGroupBuilder MapQuoteEndpoints(this RouteGroupBuilder v1)
    {
        v1.MapPost("/quote", CreateQuote)
            .WithName("CreateQuote")
            .Produces<QuoteResponse>(StatusCodes.Status200OK)
            .Produces<ApiError>(StatusCodes.Status400BadRequest)
            .Produces<ApiError>(StatusCodes.Status422UnprocessableEntity);

        v1.MapGet("/quotes/{quoteId}", GetQuote)
            .WithName("GetQuote")
            .Produces<QuoteResponse>(StatusCodes.Status200OK)
            .Produces<ApiError>(StatusCodes.Status404NotFound)
            .Produces<ApiError>(StatusCodes.Status410Gone);

        return v1;
    }

    private static IResult CreateQuote(QuoteRequest request, QuoteService service)
        => ToResult(service.CreateQuote(request));

    private static IResult GetQuote(string quoteId, QuoteService service)
        => ToResult(service.GetQuote(quoteId));

    private static IResult ToResult(QuoteOutcome outcome)
        => outcome.IsSuccess
            ? Results.Json(outcome.Quote, statusCode: outcome.StatusCode)
            : Results.Json(outcome.Error, statusCode: outcome.StatusCode);
}
