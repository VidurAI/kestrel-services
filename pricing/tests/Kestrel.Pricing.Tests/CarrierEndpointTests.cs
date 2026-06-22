using System.Net;
using System.Text.Json;
using Kestrel.Pricing.Tests.Support;

namespace Kestrel.Pricing.Tests;

public class CarrierEndpointTests : IDisposable
{
    private readonly PricingApiFactory _factory = new();

    public void Dispose() => _factory.Dispose();

    [Fact]
    public async Task GetSurcharges_ReturnsFuelPctAndRulesForCarrier()
    {
        var client = _factory.CreateClient();

        var response = await client.GetAsync("/v1/carriers/PCR/surcharges");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal("PCR", body.GetProperty("carrier_code").GetString());
        Assert.Equal(8.5m, body.GetProperty("fuel_surcharge_pct").GetDecimal());
        var types = body.GetProperty("surcharges").EnumerateArray().Select(s => s.GetProperty("type").GetString());
        Assert.Equal(new[] { "remote_area", "peak_season", "minimum_charge" }, types);
    }

    [Fact]
    public async Task GetSurcharges_ExposesRuleParametersWithSnakeCaseKeys()
    {
        var client = _factory.CreateClient();

        var body = await client.GetFromJsonAsync<JsonElement>("/v1/carriers/nsl/surcharges");

        var heavy = body.GetProperty("surcharges").EnumerateArray()
            .Single(s => s.GetProperty("code").GetString() == "NSL-HEAVY");
        Assert.Equal("500", heavy.GetProperty("parameters").GetProperty("threshold_kg").GetString());
    }

    [Fact]
    public async Task GetSurcharges_UnknownCarrierReturns404()
    {
        var client = _factory.CreateClient();

        var response = await client.GetAsync("/v1/carriers/ZZZ/surcharges");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal("unknown carrier code", body.GetProperty("error").GetString());
    }

    [Fact]
    public async Task ListCarriers_ReturnsAllThreeCarriers()
    {
        var client = _factory.CreateClient();

        var body = await client.GetFromJsonAsync<JsonElement>("/v1/carriers");

        var codes = body.EnumerateArray().Select(c => c.GetProperty("code").GetString());
        Assert.Equal(new[] { "BLX", "NSL", "PCR" }, codes);
    }
}
