using Kestrel.Pricing.Common;
using Kestrel.Pricing.Tests.Support;

namespace Kestrel.Pricing.Tests;

/// <summary>
/// Documents how the fuel surcharge rounds today. These assertions describe the CURRENT
/// behaviour (midpoints to even) and are intentionally not a statement of what is desired.
/// </summary>
public class FuelSurchargeRoundingTests
{
    [Theory]
    [InlineData("33.00", "4.12", "4.13")] // 4.125 -> even digit below, so ToEven goes down
    [InlineData("33.16", "4.14", "4.15")] // 4.145 -> same
    public void ApplyFuelSurcharge_RoundsMidpointToEven_WhereMoneyRoundWouldGoUp(
        string baseAmount, string currentResult, string moneyRoundResult)
    {
        var calculator = TestDefaults.Calculator(fuelPct: 12.5m);

        var surcharge = calculator.ApplyFuelSurcharge(decimal.Parse(baseAmount));

        Assert.Equal(decimal.Parse(currentResult), surcharge);
        Assert.Equal(decimal.Parse(moneyRoundResult), Money.Round(decimal.Parse(baseAmount) * 12.5m / 100m));
        Assert.NotEqual(Money.Round(decimal.Parse(baseAmount) * 12.5m / 100m), surcharge);
    }

    [Fact]
    public void ApplyFuelSurcharge_AgreesWithMoneyRound_WhenMidpointNeighbourIsOdd()
    {
        var calculator = TestDefaults.Calculator(fuelPct: 12.5m);

        // 33.08 * 12.5% = 4.135, the digit before the midpoint is odd so both modes round up.
        Assert.Equal(4.14m, calculator.ApplyFuelSurcharge(33.08m));
        Assert.Equal(4.14m, Money.Round(33.08m * 12.5m / 100m));
    }

    [Fact]
    public void ApplyFuelSurcharge_AgreesWithMoneyRound_ForNonMidpointValues()
    {
        var calculator = TestDefaults.Calculator(fuelPct: 8.5m);

        Assert.Equal(2.04m, calculator.ApplyFuelSurcharge(24.00m));
        Assert.Equal(0m, calculator.ApplyFuelSurcharge(0m));
    }

    [Fact]
    public void Calculate_FuelLineCarriesTheToEvenResult()
    {
        // NSL domestic 0-50 kg band: 18.00 base + 0.60/kg. 25 kg gives a 33.00 linehaul, so the
        // raw fuel value at 12.5% is exactly 4.125.
        var calculator = TestDefaults.Calculator(fuelPct: 12.5m);

        var quote = calculator.Calculate("NSL", "NLRTM", "NLAMS", Geo.Zone.Domestic, 25m);

        Assert.NotNull(quote);
        var fuel = quote!.Lines.Single(l => l.Code == "FUEL");
        Assert.Equal(4.12m, fuel.Amount);
        Assert.Equal(37.12m, quote.Amount);
    }
}
