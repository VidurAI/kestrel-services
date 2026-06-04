using Microsoft.Extensions.Options;

namespace Kestrel.Pricing.Options;

public sealed class PricingOptionsValidator : IValidateOptions<PricingOptions>
{
    public ValidateOptionsResult Validate(string? name, PricingOptions options)
    {
        var failures = new List<string>();

        if (options.FuelSurchargePct < 0m || options.FuelSurchargePct > 100m)
        {
            failures.Add("Pricing:FuelSurchargePct must be between 0 and 100");
        }

        if (string.IsNullOrWhiteSpace(options.DefaultCurrency)
            || options.DefaultCurrency.Length != 3
            || !options.DefaultCurrency.All(char.IsAsciiLetterUpper))
        {
            failures.Add("Pricing:DefaultCurrency must be a three-letter upper-case ISO 4217 code");
        }

        if (options.QuoteTtlMinutes <= 0)
        {
            failures.Add("Pricing:QuoteTtlMinutes must be positive");
        }

        if (options.MaxQuotesRetained <= 0)
        {
            failures.Add("Pricing:MaxQuotesRetained must be positive");
        }

        if (options.MaxWeightKg <= 0m)
        {
            failures.Add("Pricing:MaxWeightKg must be positive");
        }

        return failures.Count == 0
            ? ValidateOptionsResult.Success
            : ValidateOptionsResult.Fail(failures);
    }
}
