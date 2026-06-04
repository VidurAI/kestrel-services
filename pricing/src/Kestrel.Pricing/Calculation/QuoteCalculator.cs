using Kestrel.Pricing.Common;
using Kestrel.Pricing.Geo;
using Kestrel.Pricing.Models;
using Kestrel.Pricing.Options;
using Kestrel.Pricing.Surcharges;
using Kestrel.Pricing.Tariffs;
using Microsoft.Extensions.Options;

namespace Kestrel.Pricing.Calculation;

/// <summary>
/// Prices a shipment: tariff band (base plus per-kg), fuel surcharge, then the carrier's
/// surcharge rules. The breakdown lines always sum to the quote amount.
/// </summary>
public sealed class QuoteCalculator
{
    public const string BaseCode = "BASE";
    public const string WeightCode = "WEIGHT";
    public const string FuelCode = "FUEL";

    private readonly ITariffStore _tariffs;
    private readonly SurchargeEngine _surcharges;
    private readonly IOptions<PricingOptions> _options;
    private readonly TimeProvider _clock;

    public QuoteCalculator(
        ITariffStore tariffs,
        SurchargeEngine surcharges,
        IOptions<PricingOptions> options,
        TimeProvider clock)
    {
        _tariffs = tariffs;
        _surcharges = surcharges;
        _options = options;
        _clock = clock;
    }

    /// <summary>Returns null when the carrier has no tariff band for this lane and weight.</summary>
    public CalculatedQuote? Calculate(string carrierCode, string origin, string destination, Zone zone, decimal weightKg)
    {
        var chargeableKg = ChargeableWeight.Of(weightKg);
        var tariff = _tariffs.Find(carrierCode, zone, chargeableKg);
        if (tariff is null)
        {
            return null;
        }

        var baseAmount = Money.Round(tariff.BaseAmount);
        var weightAmount = Money.Round(tariff.PerKg * chargeableKg);
        var linehaul = baseAmount + weightAmount;
        var fuel = ApplyFuelSurcharge(linehaul);

        var lines = new List<BreakdownLine>
        {
            new(BaseCode, $"Base rate {ZoneCodes.ToTariffName(zone).ToLowerInvariant()} {tariff.MinKg:0.##}-{tariff.MaxKg:0.##} kg", baseAmount),
            new(WeightCode, $"Weight charge {chargeableKg:0.0#} kg", weightAmount),
            new(FuelCode, $"Fuel surcharge {_options.Value.FuelSurchargePct:0.##}%", fuel),
        };

        var context = new SurchargeContext(
            tariff.CarrierCode,
            origin,
            destination,
            zone,
            chargeableKg,
            linehaul + fuel,
            DateOnly.FromDateTime(_clock.GetUtcNow().UtcDateTime));

        lines.AddRange(_surcharges.Evaluate(context).Select(s => new BreakdownLine(s.Code, s.Description, s.Amount)));

        var total = Money.Sum(lines.Select(l => l.Amount));
        return new CalculatedQuote(tariff.CarrierCode, total, _options.Value.DefaultCurrency, lines);
    }

    public decimal ApplyFuelSurcharge(decimal baseAmount)
    {
        var value = baseAmount * _options.Value.FuelSurchargePct / 100m;
        return Math.Round(value, 2);
    }
}
