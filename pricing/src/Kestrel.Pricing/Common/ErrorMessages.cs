namespace Kestrel.Pricing.Common;

/// <summary>
/// Error strings returned in the <c>error</c> field of every non-2xx response. Downstream
/// services (gateway, web) match on these strings, so treat them as part of the contract.
/// </summary>
public static class ErrorMessages
{
    public const string UnknownCarrier = "unknown carrier code";
    public const string InvalidRequest = "invalid request";
    public const string NoTariff = "no tariff for lane and weight";
    public const string WeightExceedsCarrierLimit = "weight exceeds carrier limit";
    public const string NoCarrierAvailable = "no carrier available for lane";
    public const string QuoteNotFound = "quote not found";
    public const string QuoteExpired = "quote expired";
    public const string Internal = "internal error";
}
