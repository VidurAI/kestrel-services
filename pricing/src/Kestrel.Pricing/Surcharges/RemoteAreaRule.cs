using Kestrel.Pricing.Common;

namespace Kestrel.Pricing.Surcharges;

/// <summary>
/// Flat fee when the destination falls under one of the carrier's remote-area prefixes.
/// A prefix is a country code ("IS") or a full UN/LOCODE ("NOTOS").
/// </summary>
public sealed class RemoteAreaRule : SurchargeRuleBase
{
    private readonly string[] _prefixes;
    private readonly decimal _flatAmount;

    public RemoteAreaRule(string code, string carrierCode, IEnumerable<string> prefixes, decimal flatAmount, int order = 20)
        : base(code, carrierCode, order, "Remote area delivery surcharge")
    {
        _prefixes = prefixes.Select(p => p.ToUpperInvariant()).ToArray();
        _flatAmount = Money.Round(flatAmount);
    }

    protected override string RuleType => SurchargeTypes.RemoteArea;

    public override SurchargeLine? Evaluate(SurchargeContext context)
    {
        var destination = context.Destination.ToUpperInvariant();
        return _prefixes.Any(p => destination.StartsWith(p, StringComparison.Ordinal))
            ? Line(_flatAmount)
            : null;
    }

    protected override IReadOnlyDictionary<string, string> Parameters() => new Dictionary<string, string>
    {
        ["flat_amount"] = Format(_flatAmount),
        ["destination_prefixes"] = string.Join(",", _prefixes),
    };
}
