using Kestrel.Pricing.Surcharges;

namespace Kestrel.Pricing.Models;

public sealed record CarrierSummary(string Code, string Name, bool Active, int MaxWeightKg, int TypicalTransitDays);

/// <summary>Wire shape of <c>GET /v1/carriers/{code}/surcharges</c>.</summary>
public sealed record CarrierSurchargesResponse(
    string CarrierCode,
    string Currency,
    decimal FuelSurchargePct,
    IReadOnlyList<SurchargeDescriptor> Surcharges);
