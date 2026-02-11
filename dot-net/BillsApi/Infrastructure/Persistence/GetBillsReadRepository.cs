using BillsApi.Application.Bills.GetBills;
using Microsoft.EntityFrameworkCore;

namespace BillsApi.Infrastructure.Persistence;

public sealed class GetBillsReadRepository(ArenaDbContext dbContext) : IGetBillsReadRepository
{
    public async Task<IReadOnlyList<BillDto>> ListAsync(CancellationToken cancellationToken = default)
    {
        return await dbContext.Bills
            .AsNoTracking()
            .OrderBy(x => x.Id)
            .Select(x => new BillDto(
                x.Id,
                x.BillNumber,
                x.IssuedAt,
                (x.Lines.Select(l => (decimal?)l.LineAmount).Sum() ?? 0m) + x.Tax,
                x.Currency
            ))
            .ToListAsync(cancellationToken);
    }
}
