using Kestrel.Pricing.Geo;

namespace Kestrel.Pricing.Tariffs;

/// <summary>
/// One weight band of a carrier's tariff for a zone. A band covers <c>[MinKg, MaxKg)</c> and
/// prices a shipment as <c>BaseAmount + PerKg * chargeableWeight</c>.
/// </summary>
public sealed record TariffRow(
    string CarrierCode,
    Zone Zone,
    decimal MinKg,
    decimal MaxKg,
    decimal BaseAmount,
    decimal PerKg)
{
    public bool Covers(decimal weightKg) => weightKg >= MinKg && weightKg < MaxKg;
}
