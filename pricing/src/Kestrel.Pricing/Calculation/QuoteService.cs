using Kestrel.Pricing.Carriers;
using Kestrel.Pricing.Common;
using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Models;
using Kestrel.Pricing.Options;
using Kestrel.Pricing.Quotes;
using Kestrel.Pricing.Validation;
using Microsoft.Extensions.Options;

namespace Kestrel.Pricing.Calculation;

/// <summary>
/// Orchestrates validation, carrier selection, pricing and persistence for <c>POST /v1/quote</c>.
/// </summary>
public sealed class QuoteService
{
    private readonly QuoteRequestValidator _validator;
    private readonly ICarrierCatalog _carriers;
    private readonly IZoneResolver _zones;
    private readonly QuoteCalculator _calculator;
    private readonly IQuoteRepository _repository;
    private readonly IOptions<PricingOptions> _options;
    private readonly TimeProvider _clock;
    private readonly ILogger<QuoteService> _logger;

    public QuoteService(
        QuoteRequestValidator validator,
        ICarrierCatalog carriers,
        IZoneResolver zones,
        QuoteCalculator calculator,
        IQuoteRepository repository,
        IOptions<PricingOptions> options,
        TimeProvider clock,
        ILogger<QuoteService> logger)
    {
        _validator = validator;
        _carriers = carriers;
        _zones = zones;
        _calculator = calculator;
        _repository = repository;
        _options = options;
        _clock = clock;
        _logger = logger;
    }

    public QuoteOutcome CreateQuote(QuoteRequest? request)
    {
        var validation = _validator.Validate(request);
        if (!validation.IsValid)
        {
            return QuoteOutcome.Invalid(validation.Errors);
        }

        var input = validation.Value!;
        var zone = _zones.Resolve(input.Origin, input.Destination);

        var candidates = ResolveCandidates(input, out var rejection);
        if (rejection is not null)
        {
            return rejection;
        }

        var best = PriceCheapest(candidates, input, zone);
        if (best is null)
        {
            return QuoteOutcome.Rejected(
                input.CarrierCode is null ? ErrorMessages.NoCarrierAvailable : ErrorMessages.NoTariff);
        }

        var response = Persist(best, input);
        _logger.LogInformation(
            "issued quote {QuoteId} carrier={Carrier} zone={Zone} amount={Amount} {Currency}",
            response.QuoteId, response.CarrierCode, zone, response.Amount, response.Currency);
        return QuoteOutcome.Ok(response);
    }

    public QuoteOutcome GetQuote(string quoteId)
    {
        var stored = _repository.Find(quoteId);
        if (stored is null)
        {
            return QuoteOutcome.NotFound();
        }

        return stored.Quote.ValidUntil <= _clock.GetUtcNow()
            ? QuoteOutcome.Expired()
            : QuoteOutcome.Ok(stored.Quote);
    }

    private IReadOnlyList<Carrier> ResolveCandidates(NormalizedQuoteRequest input, out QuoteOutcome? rejection)
    {
        rejection = null;

        // Multi-carrier mode: validator only lets a missing carrier through when the flag is on.
        if (input.CarrierCode is null)
        {
            return _carriers.All().Where(c => c.Active && input.WeightKg <= c.MaxWeightKg).ToList();
        }

        var carrier = _carriers.Find(input.CarrierCode);
        if (carrier is null || !carrier.Active)
        {
            rejection = QuoteOutcome.Rejected(ErrorMessages.UnknownCarrier);
            return [];
        }

        if (input.WeightKg > carrier.MaxWeightKg)
        {
            rejection = QuoteOutcome.Rejected(ErrorMessages.WeightExceedsCarrierLimit);
            return [];
        }

        return [carrier];
    }

    private CalculatedQuote? PriceCheapest(IEnumerable<Carrier> candidates, NormalizedQuoteRequest input, Zone zone)
    {
        CalculatedQuote? best = null;
        foreach (var carrier in candidates)
        {
            var priced = _calculator.Calculate(carrier.Code, input.Origin, input.Destination, zone, input.WeightKg);
            if (priced is null)
            {
                continue;
            }

            if (best is null || priced.Amount < best.Amount)
            {
                best = priced;
            }
        }

        return best;
    }

    private QuoteResponse Persist(CalculatedQuote priced, NormalizedQuoteRequest input)
    {
        var now = _clock.GetUtcNow();
        var response = new QuoteResponse(
            NewQuoteId(),
            priced.Amount,
            priced.Currency,
            priced.Lines,
            priced.CarrierCode,
            now.AddMinutes(_options.Value.QuoteTtlMinutes));

        _repository.Add(new StoredQuote(response, input, now));
        return response;
    }

    private static string NewQuoteId() => "qt_" + Guid.NewGuid().ToString("N")[..16];
}
