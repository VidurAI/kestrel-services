namespace Kestrel.Pricing.Tariffs;

/// <summary>
/// Source of tariff rows. The implementation in use is chosen by name from configuration
/// (see <see cref="TariffLoaderFactory"/>), so implementations need a public parameterless
/// constructor.
/// </summary>
public interface ITariffLoader
{
    /// <summary>Human-readable description of where the rows come from (for logs and /healthz).</summary>
    string SourceName { get; }

    IReadOnlyList<TariffRow> Load();
}

public sealed class TariffLoadException : Exception
{
    public TariffLoadException(string message)
        : base(message)
    {
    }

    public TariffLoadException(string message, Exception inner)
        : base(message, inner)
    {
    }
}
