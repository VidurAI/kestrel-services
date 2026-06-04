namespace Kestrel.Pricing.Carriers;

public interface ICarrierCatalog
{
    /// <summary>Looks a carrier up by code (case-insensitive). Returns null when unknown.</summary>
    Carrier? Find(string? code);

    IReadOnlyList<Carrier> All();
}
