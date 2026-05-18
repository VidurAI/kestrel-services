using Kestrel.Pricing.Geo;

namespace Kestrel.Pricing.Surcharges;

/// <summary>Everything a rule may look at when deciding whether and how much to charge.</summary>
/// <param name="Subtotal">Running total of the quote so far, including surcharges from earlier rules.</param>
public sealed record SurchargeContext(
    string CarrierCode,
    string Origin,
    string Destination,
    Zone Zone,
    decimal ChargeableWeightKg,
    decimal Subtotal,
    DateOnly QuoteDate);

public sealed record SurchargeLine(string Code, string Description, decimal Amount);

/// <summary>Public, serialisable description of a rule (served by the surcharges endpoint).</summary>
public sealed record SurchargeDescriptor(
    string Code,
    string Type,
    string Description,
    IReadOnlyDictionary<string, string> Parameters);

public static class SurchargeTypes
{
    public const string WeightThreshold = "weight_threshold";
    public const string RemoteArea = "remote_area";
    public const string PeakSeason = "peak_season";
    public const string MinimumCharge = "minimum_charge";
}

public interface ISurchargeRule
{
    string Code { get; }

    string CarrierCode { get; }

    /// <summary>Evaluation order; lower runs first. Rules that read the subtotal run late.</summary>
    int Order { get; }

    /// <summary>Returns the charge, or null when the rule does not apply to this shipment.</summary>
    SurchargeLine? Evaluate(SurchargeContext context);

    SurchargeDescriptor Describe();
}
