using BillsApi.Domain.Bills;
using BillsApi.Domain.Common;
using Microsoft.EntityFrameworkCore;

namespace BillsApi.Infrastructure.Persistence;

public sealed class ArenaDbContext(DbContextOptions<ArenaDbContext> options)
    : DbContext(options), IUnitOfWork
{
    public DbSet<Bill> Bills => Set<Bill>();
    public DbSet<BillLine> BillLines => Set<BillLine>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.ApplyConfigurationsFromAssembly(typeof(ArenaDbContext).Assembly);
    }

    Task<int> IUnitOfWork.SaveChangesAsync(CancellationToken cancellationToken)
        => base.SaveChangesAsync(cancellationToken);
}
