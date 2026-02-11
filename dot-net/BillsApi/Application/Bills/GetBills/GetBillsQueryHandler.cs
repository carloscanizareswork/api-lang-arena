using BillsApi.Application.Abstractions;

namespace BillsApi.Application.Bills.GetBills;

public sealed class GetBillsQueryHandler(IGetBillsReadRepository readRepository)
    : IQueryHandler<GetBillsQuery, IReadOnlyList<BillDto>>
{
    public async Task<IReadOnlyList<BillDto>> Handle(GetBillsQuery request, CancellationToken cancellationToken)
    {
        return await readRepository.ListAsync(cancellationToken);
    }
}
