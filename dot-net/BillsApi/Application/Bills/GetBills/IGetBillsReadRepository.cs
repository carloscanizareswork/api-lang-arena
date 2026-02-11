namespace BillsApi.Application.Bills.GetBills;

public interface IGetBillsReadRepository
{
    Task<IReadOnlyList<BillDto>> ListAsync(CancellationToken cancellationToken = default);
}
