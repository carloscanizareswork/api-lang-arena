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
    public string Currency { get; private set; } = "USD";
    public IReadOnlyCollection<BillLine> Lines => _lines.AsReadOnly();

    public decimal CalculateTotal()
    {
        return _lines.Sum(x => x.LineAmount) + Tax;
    }
}
