namespace Kestrel.Pricing.Options;

/// <summary>
/// Bound from the <c>Pricing</c> configuration section.
/// </summary>
public sealed class PricingOptions
{
    public const string SectionName = "Pricing";

    /// <summary>Pricing:FuelSurchargePct. Percentage applied to the linehaul amount.</summary>
    public decimal FuelSurchargePct { get; set; } = 8.5m;

    /// <summary>Pricing:DefaultCurrency. ISO 4217 code used on every quote.</summary>
    public string DefaultCurrency { get; set; } = "EUR";

    /// <summary>How long an issued quote stays valid.</summary>
    public int QuoteTtlMinutes { get; set; } = 30;

    /// <summary>Upper bound on quotes held by the in-memory repository.</summary>
    public int MaxQuotesRetained { get; set; } = 10_000;

    /// <summary>Global weight ceiling, independent of per-carrier limits.</summary>
    public decimal MaxWeightKg { get; set; } = 20_000m;

    /// <summary>Pricing:Features. Runtime feature flags.</summary>
    public FeatureOptions Features { get; set; } = new();
}

public sealed class FeatureOptions
{
    /// <summary>
    /// Pricing:Features:MultiCarrierQuotes. When on, a request without carrier_code is quoted
    /// against every active carrier and the cheapest quote is returned.
    /// </summary>
    public bool MultiCarrierQuotes { get; set; }
}
