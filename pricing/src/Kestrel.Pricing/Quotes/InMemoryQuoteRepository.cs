using Kestrel.Pricing.Options;
using Microsoft.Extensions.Options;

namespace Kestrel.Pricing.Quotes;

/// <summary>
/// Bounded in-memory quote store. When the capacity is exceeded the oldest quotes are evicted.
/// Quotes are short-lived (see <c>Pricing:QuoteTtlMinutes</c>) so losing them on restart is accepted.
/// </summary>
public sealed class InMemoryQuoteRepository : IQuoteRepository
{
    private readonly object _gate = new();
    private readonly Dictionary<string, StoredQuote> _byId = new(StringComparer.Ordinal);
    private readonly LinkedList<string> _order = new();
    private readonly int _capacity;

    public InMemoryQuoteRepository(IOptions<PricingOptions> options)
        : this(options.Value.MaxQuotesRetained)
    {
    }

    public InMemoryQuoteRepository(int capacity)
    {
        if (capacity <= 0)
        {
            throw new ArgumentOutOfRangeException(nameof(capacity));
        }

        _capacity = capacity;
    }

    public int Count
    {
        get
        {
            lock (_gate)
            {
                return _byId.Count;
            }
        }
    }

    public void Add(StoredQuote quote)
    {
        lock (_gate)
        {
            var id = quote.Quote.QuoteId;
            if (_byId.ContainsKey(id))
            {
                throw new InvalidOperationException($"quote {id} already exists");
            }

            _byId[id] = quote;
            _order.AddLast(id);

            while (_byId.Count > _capacity)
            {
                var oldest = _order.First!.Value;
                _order.RemoveFirst();
                _byId.Remove(oldest);
            }
        }
    }

    public StoredQuote? Find(string quoteId)
    {
        lock (_gate)
        {
            return _byId.TryGetValue(quoteId, out var quote) ? quote : null;
        }
    }

    public IReadOnlyList<StoredQuote> Recent(int take)
    {
        lock (_gate)
        {
            var result = new List<StoredQuote>(Math.Min(take, _byId.Count));
            for (var node = _order.Last; node is not null && result.Count < take; node = node.Previous)
            {
                result.Add(_byId[node.Value]);
            }

            return result;
        }
    }
}
