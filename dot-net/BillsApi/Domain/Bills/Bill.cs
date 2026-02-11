using BillsApi.Domain.Common;

namespace BillsApi.Domain.Bills;

public sealed class Bill : AggregateRoot<long>
{
    private readonly List<BillLine> _lines = [];

    private Bill() { }

    public string BillNumber { get; private set; } = string.Empty;
    public DateOnly IssuedAt { get; private set; }
    public string CustomerName { get; private set; } = string.Empty;
    public decimal Subtotal { get; private set; }
    public decimal Tax { get; private set; }
    public string Currency { get; private set; } = "USD";
    public IReadOnlyCollection<BillLine> Lines => _lines.AsReadOnly();

    public static Bill Create(
        string billNumber,
        DateOnly issuedAt,
        string customerName,
        string currency,
        decimal tax)
    {
        if (string.IsNullOrWhiteSpace(billNumber))
        {
            throw new DomainException("Bill number is required.");
        }

        if (string.IsNullOrWhiteSpace(customerName))
        {
            throw new DomainException("Customer name is required.");
        }

        if (string.IsNullOrWhiteSpace(currency) || currency.Length != 3)
        {
            throw new DomainException("Currency must be a 3-letter ISO code.");
        }

        if (tax < 0m)
        {
            throw new DomainException("Tax cannot be negative.");
        }

        return new Bill
        {
            BillNumber = billNumber.Trim(),
            IssuedAt = issuedAt,
            CustomerName = customerName.Trim(),
            Currency = currency.Trim().ToUpperInvariant(),
            Tax = tax
        };
    }

    public void AddLine(string concept, decimal quantity, decimal unitAmount)
    {
        var lineNo = _lines.Count + 1;
        var line = BillLine.Create(lineNo, concept, quantity, unitAmount);
        _lines.Add(line);
        RecalculateSubtotal();
    }

    public decimal CalculateTotal()
    {
        return Subtotal + Tax;
    }

    private void RecalculateSubtotal()
    {
        Subtotal = _lines.Sum(x => x.LineAmount);
    }
}
