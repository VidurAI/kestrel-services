namespace Kestrel.Pricing.Tests.Support;

public static class RepoPaths
{
    /// <summary>Locates the web project's appsettings.json by walking up from the test binaries.</summary>
    public static string AppSettings()
    {
        var dir = new DirectoryInfo(AppContext.BaseDirectory);
        while (dir is not null)
        {
            var candidate = Path.Combine(dir.FullName, "src", "Kestrel.Pricing", "appsettings.json");
            if (File.Exists(candidate))
            {
                return candidate;
            }

            dir = dir.Parent;
        }

        throw new FileNotFoundException("could not locate src/Kestrel.Pricing/appsettings.json");
    }
}
