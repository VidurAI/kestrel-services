namespace Kestrel.Pricing.Quotes;

public interface IQuoteRepository
{
    int Count { get; }

    void Add(StoredQuote quote);

    StoredQuote? Find(string quoteId);

    /// <summary>Most recent quotes first.</summary>
    IReadOnlyList<StoredQuote> Recent(int take);
}
