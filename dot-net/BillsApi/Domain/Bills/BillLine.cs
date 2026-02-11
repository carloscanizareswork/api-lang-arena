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
}
