namespace BillsApi.Application.Bills.GetBills;

public sealed record BillDto(
    long Id,
    string BillNumber,
    DateOnly IssuedAt,
    decimal Total,
    string Currency
);
