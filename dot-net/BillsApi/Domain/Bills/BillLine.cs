using BillsApi.Domain.Common;

namespace BillsApi.Domain.Bills;

public sealed class BillLine : Entity<long>
{
    private BillLine() { }

    public long BillId { get; private set; }
    public int LineNo { get; private set; }
    public string Concept { get; private set; } = string.Empty;
    public decimal Quantity { get; private set; }
    public decimal UnitAmount { get; private set; }
    public decimal LineAmount { get; private set; }

    public static BillLine Create(int lineNo, string concept, decimal quantity, decimal unitAmount)
    {
        if (lineNo <= 0)
        {
            throw new DomainException("Line number must be greater than zero.");
        }

        if (string.IsNullOrWhiteSpace(concept))
        {
            throw new DomainException("Line concept is required.");
        }

        if (quantity <= 0m)
        {
            throw new DomainException("Line quantity must be greater than zero.");
        }

        if (unitAmount < 0m)
        {
            throw new DomainException("Unit amount cannot be negative.");
        }

        var amount = decimal.Round(quantity * unitAmount, 2, MidpointRounding.AwayFromZero);

        return new BillLine
        {
            LineNo = lineNo,
            Concept = concept.Trim(),
            Quantity = quantity,
            UnitAmount = unitAmount,
            LineAmount = amount
        };
    }
}
