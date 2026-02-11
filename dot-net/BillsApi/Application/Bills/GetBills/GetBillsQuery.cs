using BillsApi.Application.Abstractions;

namespace BillsApi.Application.Bills.GetBills;

public sealed record GetBillsQuery : IQuery<IReadOnlyList<BillDto>>;
