using BillsApi.Domain.Bills;
using Microsoft.EntityFrameworkCore;

namespace BillsApi.Infrastructure.Persistence;

public sealed class BillRepository(ArenaDbContext dbContext) : IBillRepository
{
    public async Task<IReadOnlyList<Bill>> ListAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Bills
            .AsNoTracking()
            .Include(x => x.Lines)
            .OrderBy(x => x.Id)
            .ToListAsync(cancellationToken);
    }

    public Task<bool> ExistsByBillNumberAsync(string billNumber, CancellationToken cancellationToken = default)
    {
        return dbContext.Bills.AsNoTracking().AnyAsync(x => x.BillNumber == billNumber, cancellationToken);
    }

    public async Task AddAsync(Bill bill, CancellationToken cancellationToken = default)
    {
        await dbContext.Bills.AddAsync(bill, cancellationToken);
    }
}
