using System.Net;
using System.Text.Json;
using Kestrel.Pricing.Tests.Support;

namespace Kestrel.Pricing.Tests;

public class QuoteEndpointTests : IDisposable
{
    private readonly PricingApiFactory _factory = new();

    public void Dispose() => _factory.Dispose();

    private static object Request(string carrier = "NSL", decimal weight = 10m)
        => new { origin = "NLRTM", destination = "NLAMS", weight_kg = weight, carrier_code = carrier };

    [Fact]
    public async Task PostQuote_ReturnsSnakeCaseQuoteWithBreakdown()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/v1/quote", Request());

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.StartsWith("qt_", body.GetProperty("quote_id").GetString());
        Assert.Equal(26.04m, body.GetProperty("amount").GetDecimal());
        Assert.Equal("EUR", body.GetProperty("currency").GetString());
        Assert.Equal("NSL", body.GetProperty("carrier_code").GetString());
        var codes = body.GetProperty("breakdown").EnumerateArray().Select(l => l.GetProperty("code").GetString());
        Assert.Equal(new[] { "BASE", "WEIGHT", "FUEL" }, codes);
    }

    [Fact]
    public async Task PostQuote_BreakdownSumsToAmount()
    {
        var client = _factory.CreateClient();

        var body = await (await client.PostAsJsonAsync("/v1/quote", Request("BLX", 321.7m)))
            .Content.ReadFromJsonAsync<JsonElement>();

        var sum = body.GetProperty("breakdown").EnumerateArray().Sum(l => l.GetProperty("amount").GetDecimal());
        Assert.Equal(body.GetProperty("amount").GetDecimal(), sum);
    }

    [Fact]
    public async Task PostQuote_UnknownCarrierReturns422()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/v1/quote", Request("ZZZ"));

        Assert.Equal((HttpStatusCode)422, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal("unknown carrier code", body.GetProperty("error").GetString());
    }

    [Fact]
    public async Task PostQuote_CarrierCodeIsCaseInsensitive()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/v1/quote", Request("nsl"));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task PostQuote_InvalidBodyReturns400WithFieldDetails()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/v1/quote", new { origin = "NLRTM", weight_kg = -1 });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal("invalid request", body.GetProperty("error").GetString());
        var fields = body.GetProperty("details").EnumerateArray().Select(d => d.GetProperty("field").GetString());
        Assert.Contains("destination", fields);
        Assert.Contains("weight_kg", fields);
    }

    [Fact]
    public async Task PostQuote_WeightAboveCarrierLimitReturns422()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/v1/quote", Request("BLX", 15_000m));

        Assert.Equal((HttpStatusCode)422, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<JsonElement>();
        Assert.Equal("weight exceeds carrier limit", body.GetProperty("error").GetString());
    }

    [Fact]
    public async Task PostQuote_MalformedJsonReturns400()
    {
        var client = _factory.CreateClient();
        var content = new StringContent("{not json", System.Text.Encoding.UTF8, "application/json");

        var response = await client.PostAsync("/v1/quote", content);

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task GetQuote_ReturnsStoredQuoteThenGoneAfterExpiry()
    {
        var client = _factory.CreateClient();
        var created = await (await client.PostAsJsonAsync("/v1/quote", Request()))
            .Content.ReadFromJsonAsync<JsonElement>();
        var id = created.GetProperty("quote_id").GetString();

        var found = await client.GetAsync($"/v1/quotes/{id}");
        _factory.Clock.Advance(TimeSpan.FromMinutes(31));
        var expired = await client.GetAsync($"/v1/quotes/{id}");
        var missing = await client.GetAsync("/v1/quotes/qt_nope");

        Assert.Equal(HttpStatusCode.OK, found.StatusCode);
        Assert.Equal(HttpStatusCode.Gone, expired.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, missing.StatusCode);
    }
}
