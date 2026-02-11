namespace BillsApi.Domain.Bills;

public sealed record Money(decimal Amount, string Currency)
{
    public static readonly Money Empty = new(0m, "USD");
}
