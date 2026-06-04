using System.Reflection;

namespace Kestrel.Pricing.Tariffs;

/// <summary>Access to the tariff files embedded in this assembly.</summary>
internal static class EmbeddedData
{
    public static TextReader Open(string logicalName)
    {
        var assembly = typeof(EmbeddedData).Assembly;
        var stream = assembly.GetManifestResourceStream(logicalName)
            ?? throw new TariffLoadException($"embedded tariff resource '{logicalName}' not found");
        return new StreamReader(stream);
    }
}
