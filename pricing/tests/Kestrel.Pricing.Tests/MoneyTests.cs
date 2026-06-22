using Kestrel.Pricing.Common;

namespace Kestrel.Pricing.Tests;

public class MoneyTests
{
    [Theory]
    [InlineData("0.125", "0.13")]
    [InlineData("0.135", "0.14")]
    [InlineData("2.345", "2.35")]
    [InlineData("-0.125", "-0.13")]
    [InlineData("1.004", "1.00")]
    public void Round_RoundsMidpointsAwayFromZero(string input, string expected)
    {
        Assert.Equal(decimal.Parse(expected), Money.Round(decimal.Parse(input)));
    }

    [Fact]
    public void ToMinorUnits_ConvertsMajorUnits()
    {
        Assert.Equal(1234L, Money.ToMinorUnits(12.34m));
        Assert.Equal(1L, Money.ToMinorUnits(0.005m));
    }

    [Fact]
    public void FromMinorUnits_IsInverseOfToMinorUnits()
    {
        Assert.Equal(12.34m, Money.FromMinorUnits(Money.ToMinorUnits(12.34m)));
    }

    [Fact]
    public void Percent_AppliesPercentageAndRounds()
    {
        Assert.Equal(2.04m, Money.Percent(24.00m, 8.5m));
        Assert.Equal(0.13m, Money.Percent(1.00m, 12.5m));
    }

    [Fact]
    public void Sum_NormalisesToTwoDecimals()
    {
        var total = Money.Sum([10.001m, 5.004m]);
        Assert.Equal(15.01m, total);
        Assert.True(Money.IsNormalised(total));
    }

    [Theory]
    [InlineData("0.5", "0.5")]
    [InlineData("0.4", "0.5")]
    [InlineData("10", "10")]
    [InlineData("10.01", "10.5")]
    public void ChargeableWeight_RoundsUpToHalfKilo(string actual, string expected)
    {
        Assert.Equal(decimal.Parse(expected), ChargeableWeight.Of(decimal.Parse(actual)));
    }

    [Fact]
    public void ChargeableWeight_RejectsNonPositiveWeight()
    {
        Assert.Throws<ArgumentOutOfRangeException>(() => ChargeableWeight.Of(0m));
    }
}
