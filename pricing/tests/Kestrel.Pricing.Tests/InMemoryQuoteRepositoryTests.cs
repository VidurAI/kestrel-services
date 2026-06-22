using Kestrel.Pricing.Models;
using Kestrel.Pricing.Quotes;

namespace Kestrel.Pricing.Tests;

public class InMemoryQuoteRepositoryTests
{
    private static StoredQuote Quote(string id)
    {
        var response = new QuoteResponse(id, 10m, "EUR", [], "NSL", DateTimeOffset.UnixEpoch);
        return new StoredQuote(response, new NormalizedQuoteRequest("NLRTM", "NLAMS", 1m, "NSL"), DateTimeOffset.UnixEpoch);
    }

    [Fact]
    public void AddThenFind_ReturnsTheStoredQuote()
    {
        var repo = new InMemoryQuoteRepository(10);
        var quote = Quote("qt_1");

        repo.Add(quote);

        Assert.Same(quote, repo.Find("qt_1"));
        Assert.Equal(1, repo.Count);
        Assert.Null(repo.Find("qt_missing"));
    }

    [Fact]
    public void Add_RejectsDuplicateIds()
    {
        var repo = new InMemoryQuoteRepository(10);
        repo.Add(Quote("qt_1"));

        Assert.Throws<InvalidOperationException>(() => repo.Add(Quote("qt_1")));
    }

    [Fact]
    public void Add_EvictsOldestWhenCapacityExceeded()
    {
        var repo = new InMemoryQuoteRepository(2);

        repo.Add(Quote("qt_1"));
        repo.Add(Quote("qt_2"));
        repo.Add(Quote("qt_3"));

        Assert.Equal(2, repo.Count);
        Assert.Null(repo.Find("qt_1"));
        Assert.NotNull(repo.Find("qt_3"));
    }

    [Fact]
    public void Recent_ReturnsNewestFirstAndHonoursTake()
    {
        var repo = new InMemoryQuoteRepository(10);
        foreach (var id in new[] { "qt_1", "qt_2", "qt_3" })
        {
            repo.Add(Quote(id));
        }

        Assert.Equal(new[] { "qt_3", "qt_2" }, repo.Recent(2).Select(q => q.Quote.QuoteId));
        Assert.Equal(3, repo.Recent(50).Count);
    }

    [Fact]
    public void Constructor_RejectsNonPositiveCapacity()
    {
        Assert.Throws<ArgumentOutOfRangeException>(() => new InMemoryQuoteRepository(0));
    }

    [Fact]
    public async Task Add_IsSafeUnderConcurrentWriters()
    {
        var repo = new InMemoryQuoteRepository(1000);

        await Task.WhenAll(Enumerable.Range(0, 8).Select(worker => Task.Run(() =>
        {
            for (var i = 0; i < 100; i++)
            {
                repo.Add(Quote($"qt_{worker}_{i}"));
            }
        })));

        Assert.Equal(800, repo.Count);
    }
}
