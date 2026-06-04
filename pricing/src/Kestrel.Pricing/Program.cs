using Kestrel.Pricing.Endpoints;

var builder = WebApplication.CreateBuilder(args);
builder.Services.AddPricing(builder.Configuration);

var app = builder.Build();
app.MapPricingEndpoints();
app.Run();

// Exposed so the integration tests can host the service with WebApplicationFactory<Program>.
public partial class Program;
