namespace Kestrel.Pricing.Surcharges;

/// <summary>
/// Applies the registered <see cref="ISurchargeRule"/>s for a carrier in order. Each rule sees
/// a subtotal that already includes the charges produced by the rules before it.
/// </summary>
public sealed class SurchargeEngine
{
    private readonly ILookup<string, ISurchargeRule> _rulesByCarrier;

    public SurchargeEngine(IEnumerable<ISurchargeRule> rules)
    {
        var ordered = rules
            .OrderBy(r => r.Order)
            .ThenBy(r => r.Code, StringComparer.Ordinal)
            .ToList();

        var duplicate = ordered
            .GroupBy(r => (r.CarrierCode, r.Code))
            .FirstOrDefault(g => g.Count() > 1);
        if (duplicate is not null)
        {
            throw new InvalidOperationException(
                $"surcharge rule '{duplicate.Key.Code}' registered twice for carrier {duplicate.Key.CarrierCode}");
        }

        _rulesByCarrier = ordered.ToLookup(r => r.CarrierCode, StringComparer.OrdinalIgnoreCase);
    }

    public IReadOnlyList<SurchargeLine> Evaluate(SurchargeContext context)
    {
        var lines = new List<SurchargeLine>();
        var running = context;

        foreach (var rule in _rulesByCarrier[context.CarrierCode])
        {
            var line = rule.Evaluate(running);
            if (line is null)
            {
                continue;
            }

            lines.Add(line);
            running = running with { Subtotal = running.Subtotal + line.Amount };
        }

        return lines;
    }

    public IReadOnlyList<SurchargeDescriptor> Describe(string carrierCode)
        => _rulesByCarrier[carrierCode].Select(r => r.Describe()).ToList();
}
