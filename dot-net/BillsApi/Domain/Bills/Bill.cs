using BillsApi.Domain.Common;

namespace BillsApi.Domain.Bills;

public sealed class Bill : AggregateRoot<long>
{
    private readonly List<BillLine> _lines = [];

    private Bill() { }

    public string BillNumber { get; private set; } = string.Empty;
    public DateOnly IssuedAt { get; private set; }
    public decimal Subtotal { get; private set; }
    public decimal Tax { get; private set; }
    public Money Total { get; private set; } = Money.Empty;
    public IReadOnlyCollection<BillLine> Lines => _lines.AsReadOnly();
}
