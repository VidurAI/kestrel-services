using System.Net;
using System.Text.Json;
using Kestrel.Pricing.Tests.Support;

namespace Kestrel.Pricing.Tests;

/// <summary>Behaviour behind the Pricing:Features:MultiCarrierQuotes flag.</summary>
public class MultiCarrierQuoteTests
{
    private static readonly object NoCarrier = new { origin = "NLRTM", destination = "NLAMS", weight_kg = 10m };

    [Fact]
    public async Task FlagOff_MissingCarrierIsRejected()
    {
        using var factory = new PricingApiFactory();
        var client = factory.CreateClient();

        var response = await client.PostAsJsonAsync("/v1/quote", NoCarrier);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal("carrier_code", body.GetProperty("details")[0].GetProperty("field").GetString());
    }

    [Fact]
    public async Task FlagOn_MissingCarrierReturnsCheapestCarrier()
    {
        using var factory = new PricingApiFactory();
        factory.ConfigOverrides["Pricing:Features:MultiCarrierQuotes"] = "true";
        var client = factory.CreateClient();

        var response = await client.PostAsJsonAsync("/v1/quote", NoCarrier);

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal("BLX", body.GetProperty("carrier_code").GetString()); // 16.56 + 5.70 + 8.5% fuel
        Assert.Equal(24.15m, body.GetProperty("amount").GetDecimal());
    }

    [Fact]
    public async Task FlagOn_ExplicitCarrierStillWins()
    {
        using var factory = new PricingApiFactory();
        factory.ConfigOverrides["Pricing:Features:MultiCarrierQuotes"] = "true";
        var client = factory.CreateClient();

        var body = await (await client.PostAsJsonAsync(
                "/v1/quote",
                new { origin = "NLRTM", destination = "NLAMS", weight_kg = 10m, carrier_code = "PCR" }))
            .Content.ReadFromJsonAsync<JsonElement>();

        Assert.Equal("PCR", body.GetProperty("carrier_code").GetString());
    }

    [Fact]
    public async Task FlagOn_SkipsCarriersThatCannotCarryTheWeight()
    {
        using var factory = new PricingApiFactory();
        factory.ConfigOverrides["Pricing:Features:MultiCarrierQuotes"] = "true";
        var client = factory.CreateClient();

        var body = await (await client.PostAsJsonAsync(
                "/v1/quote",
                new { origin = "NLRTM", destination = "NLAMS", weight_kg = 15_000m }))
            .Content.ReadFromJsonAsync<JsonElement>();

        Assert.NotEqual("BLX", body.GetProperty("carrier_code").GetString());
    }
}
