namespace BillsApi.Presentation.Contracts;

public sealed record CreateBillLineRequest(string Concept, decimal Quantity, decimal UnitAmount);

public sealed record CreateBillRequest(
    string BillNumber,
    DateOnly IssuedAt,
    string CustomerName,
    string Currency,
    decimal Tax,
    IReadOnlyList<CreateBillLineRequest> Lines
);
