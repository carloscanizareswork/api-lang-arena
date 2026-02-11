using BillsApi.Application.Abstractions;
using BillsApi.Domain.Bills;

namespace BillsApi.Application.Bills.GetBills;

public sealed class GetBillsQueryHandler(IBillRepository billRepository)
    : IQueryHandler<GetBillsQuery, IReadOnlyList<BillDto>>
{
    public async Task<IReadOnlyList<BillDto>> Handle(GetBillsQuery request, CancellationToken cancellationToken)
    {
        var bills = await billRepository.ListAsync(cancellationToken);

        return bills
            .Select(b => new BillDto(
                b.Id,
                b.BillNumber,
                b.IssuedAt,
                b.Total.Amount,
                b.Total.Currency
            ))
            .ToList();
    }
}
