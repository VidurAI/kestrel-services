using Kestrel.Pricing.Common;

namespace Kestrel.Pricing.Surcharges;

/// <summary>
/// Percentage uplift on the running subtotal while the quote date falls inside a seasonal window.
/// The window may wrap the year end (for example 15 October to 5 January).
/// </summary>
public sealed class PeakSeasonRule : SurchargeRuleBase
{
    private readonly (int Month, int Day) _start;
    private readonly (int Month, int Day) _end;
    private readonly decimal _pct;

    public PeakSeasonRule(
        string code,
        string carrierCode,
        (int Month, int Day) start,
        (int Month, int Day) end,
        decimal pct,
        int order = 30)
        : base(code, carrierCode, order, "Peak season surcharge")
    {
        _start = start;
        _end = end;
        _pct = pct;
    }

    protected override string RuleType => SurchargeTypes.PeakSeason;

    public bool IsInSeason(DateOnly date)
    {
        var current = (date.Month, date.Day);
        return CompareTo(_start, _end) <= 0
            ? CompareTo(current, _start) >= 0 && CompareTo(current, _end) <= 0
            : CompareTo(current, _start) >= 0 || CompareTo(current, _end) <= 0;
    }

    public override SurchargeLine? Evaluate(SurchargeContext context)
    {
        if (!IsInSeason(context.QuoteDate))
        {
            return null;
        }

        var amount = Money.Percent(context.Subtotal, _pct);
        return amount > 0m ? Line(amount) : null;
    }

    protected override IReadOnlyDictionary<string, string> Parameters() => new Dictionary<string, string>
    {
        ["pct"] = Format(_pct),
        ["starts"] = $"{_start.Month:00}-{_start.Day:00}",
        ["ends"] = $"{_end.Month:00}-{_end.Day:00}",
    };

    private static int CompareTo((int Month, int Day) a, (int Month, int Day) b)
        => a.Month != b.Month ? a.Month.CompareTo(b.Month) : a.Day.CompareTo(b.Day);
}
