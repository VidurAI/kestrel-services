using Microsoft.Extensions.Configuration;

namespace Kestrel.Pricing.Tariffs;

/// <summary>
/// Resolves the configured <see cref="ITariffLoader"/> by type name. The name comes from the
/// top-level <c>TariffLoader</c> key in appsettings.json so that operations can switch
/// tariff sources without a rebuild; implementations are therefore never referenced in code.
/// </summary>
public static class TariffLoaderFactory
{
    public const string ConfigKey = "TariffLoader";
    public const string DefaultTypeName = "Kestrel.Pricing.Tariffs.CsvTariffLoader";

    public static ITariffLoader CreateFromConfiguration(IConfiguration configuration)
        => Create(configuration[ConfigKey]);

    public static ITariffLoader Create(string? typeName)
    {
        var name = string.IsNullOrWhiteSpace(typeName) ? DefaultTypeName : typeName.Trim();

        var type = Type.GetType(name, throwOnError: false);
        if (type is null)
        {
            throw new TariffLoadException($"tariff loader type '{name}' could not be resolved");
        }

        if (!typeof(ITariffLoader).IsAssignableFrom(type) || type.IsAbstract || type.IsInterface)
        {
            throw new TariffLoadException($"type '{name}' is not a concrete {nameof(ITariffLoader)}");
        }

        try
        {
            return (ITariffLoader)Activator.CreateInstance(type)!;
        }
        catch (MissingMethodException ex)
        {
            throw new TariffLoadException($"tariff loader '{name}' has no public parameterless constructor", ex);
        }
    }
}
