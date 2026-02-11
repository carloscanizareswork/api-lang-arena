using BillsApi.Domain.Bills;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace BillsApi.Infrastructure.Persistence.Configurations;

public sealed class BillLineConfiguration : IEntityTypeConfiguration<BillLine>
{
    public void Configure(EntityTypeBuilder<BillLine> builder)
    {
        builder.ToTable("bill_line");

        builder.HasKey(x => x.Id);
        builder.Property(x => x.Id).HasColumnName("id");
        builder.Property(x => x.BillId).HasColumnName("bill_id").IsRequired();
        builder.Property(x => x.LineNo).HasColumnName("line_no").IsRequired();
        builder.Property(x => x.Concept).HasColumnName("concept").HasMaxLength(200).IsRequired();
        builder.Property(x => x.Quantity).HasColumnName("quantity").HasPrecision(10, 2).IsRequired();
        builder.Property(x => x.UnitAmount).HasColumnName("unit_amount").HasPrecision(12, 2).IsRequired();
        builder.Property(x => x.LineAmount).HasColumnName("line_amount").HasPrecision(12, 2).IsRequired();
    }
}
