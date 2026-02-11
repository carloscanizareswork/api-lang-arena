using BillsApi.Application.Abstractions;

namespace BillsApi.Application.Bills.CreateBill;

public sealed record CreateBillLineInput(string Concept, decimal Quantity, decimal UnitAmount);

public sealed record CreateBillCommand(
    string BillNumber,
    DateOnly IssuedAt,
    string CustomerName,
    string Currency,
    decimal Tax,
    IReadOnlyList<CreateBillLineInput> Lines) : ICommand<CreateBillResult>;
