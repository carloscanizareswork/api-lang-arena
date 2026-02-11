namespace BillsApi.Application.Bills.CreateBill;

public sealed record BillCreatedIntegrationEvent(
    long BillId,
    string BillNumber,
    DateOnly IssuedAt,
    decimal Subtotal,
    decimal Tax,
    decimal Total,
    string Currency,
    DateTime OccurredAtUtc
);
