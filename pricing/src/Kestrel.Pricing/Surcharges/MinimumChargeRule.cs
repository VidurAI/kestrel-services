using Kestrel.Pricing.Common;

namespace Kestrel.Pricing.Surcharges;

/// <summary>
/// Tops the quote up to the carrier's minimum charge. Runs last so it sees the full subtotal.
/// </summary>
public sealed class MinimumChargeRule : SurchargeRuleBase
{
    private readonly decimal _minimum;

    public MinimumChargeRule(string code, string carrierCode, decimal minimum, int order = 100)
        : base(code, carrierCode, order, "Minimum charge top-up")
    {
        _minimum = Money.Round(minimum);
    }

    protected override string RuleType => SurchargeTypes.MinimumCharge;

    public override SurchargeLine? Evaluate(SurchargeContext context)
    {
        var shortfall = _minimum - context.Subtotal;
        return shortfall > 0m ? Line(Money.Round(shortfall)) : null;
    }

    protected override IReadOnlyDictionary<string, string> Parameters() => new Dictionary<string, string>
    {
        ["minimum_amount"] = Format(_minimum),
    };
}
