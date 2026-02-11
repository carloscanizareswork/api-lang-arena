namespace BillsApi.Domain.Bills;

public interface IBillRepository
{
    Task<IReadOnlyList<Bill>> ListAsync(CancellationToken cancellationToken = default);
    Task<bool> ExistsByBillNumberAsync(string billNumber, CancellationToken cancellationToken = default);
    Task AddAsync(Bill bill, CancellationToken cancellationToken = default);
}
