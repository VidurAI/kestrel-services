using System.Globalization;

namespace Kestrel.Pricing.Surcharges;

public abstract class SurchargeRuleBase : ISurchargeRule
{
    protected SurchargeRuleBase(string code, string carrierCode, int order, string description)
    {
        Code = code;
        CarrierCode = carrierCode.ToUpperInvariant();
        Order = order;
        Description = description;
    }

    public string Code { get; }

    public string CarrierCode { get; }

    public int Order { get; }

    protected string Description { get; }

    protected abstract string RuleType { get; }

    public abstract SurchargeLine? Evaluate(SurchargeContext context);

    public SurchargeDescriptor Describe()
        => new(Code, RuleType, Description, Parameters());

    /// <summary>Rule-specific settings exposed to API consumers. Keys are snake_case.</summary>
    protected abstract IReadOnlyDictionary<string, string> Parameters();

    protected SurchargeLine Line(decimal amount) => new(Code, Description, amount);

    protected static string Format(decimal value)
        => value.ToString("0.##", CultureInfo.InvariantCulture);
}
