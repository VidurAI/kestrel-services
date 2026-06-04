using Kestrel.Pricing.Common;

namespace Kestrel.Pricing.Surcharges;

/// <summary>Charges a per-kilogram fee for every kilogram above a heavy-lift threshold.</summary>
public sealed class WeightThresholdRule : SurchargeRuleBase
{
    private readonly decimal _thresholdKg;
    private readonly decimal _perKgOver;

    public WeightThresholdRule(string code, string carrierCode, decimal thresholdKg, decimal perKgOver, int order = 10)
        : base(code, carrierCode, order, "Heavy shipment surcharge")
    {
        _thresholdKg = thresholdKg;
        _perKgOver = perKgOver;
    }

    protected override string RuleType => SurchargeTypes.WeightThreshold;

    public override SurchargeLine? Evaluate(SurchargeContext context)
    {
        if (context.ChargeableWeightKg <= _thresholdKg)
        {
            return null;
        }

        var amount = Money.Round((context.ChargeableWeightKg - _thresholdKg) * _perKgOver);
        return amount > 0m ? Line(amount) : null;
    }

    protected override IReadOnlyDictionary<string, string> Parameters() => new Dictionary<string, string>
    {
        ["threshold_kg"] = Format(_thresholdKg),
        ["per_kg_over"] = Format(_perKgOver),
    };
}
