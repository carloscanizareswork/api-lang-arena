namespace BillsApi.Domain.Bills;

public interface IBillRepository
{
    Task<IReadOnlyList<Bill>> ListAsync(CancellationToken cancellationToken = default);
}
