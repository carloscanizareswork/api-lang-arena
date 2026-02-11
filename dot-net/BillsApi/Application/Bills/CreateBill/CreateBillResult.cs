namespace BillsApi.Application.Bills.CreateBill;

public sealed record CreateBillResult(
    long Id,
    string BillNumber,
    DateOnly IssuedAt,
    decimal Subtotal,
    decimal Tax,
    decimal Total,
    string Currency
);
