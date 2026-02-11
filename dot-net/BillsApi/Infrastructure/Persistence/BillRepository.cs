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
}
