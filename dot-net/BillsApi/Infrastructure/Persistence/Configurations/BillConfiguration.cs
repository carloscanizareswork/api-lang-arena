using BillsApi.Domain.Bills;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Metadata.Builders;

namespace BillsApi.Infrastructure.Persistence.Configurations;

public sealed class BillConfiguration : IEntityTypeConfiguration<Bill>
{
    public void Configure(EntityTypeBuilder<Bill> builder)
    {
        builder.ToTable("bill");

        builder.HasKey(x => x.Id);
        builder.Property(x => x.Id).HasColumnName("id");
        builder.Property(x => x.BillNumber).HasColumnName("bill_number").HasMaxLength(50).IsRequired();
        builder.Property(x => x.IssuedAt).HasColumnName("issued_at").IsRequired();
        builder.Property(x => x.Subtotal).HasColumnName("subtotal").HasPrecision(12, 2);
        builder.Property(x => x.Tax).HasColumnName("tax").HasPrecision(12, 2);

        builder.OwnsOne(x => x.Total, money =>
        {
            money.Property(x => x.Amount).HasColumnName("total").HasPrecision(12, 2).IsRequired();
            money.Property(x => x.Currency).HasColumnName("currency").HasMaxLength(3).IsRequired();
        });

        builder.HasMany(x => x.Lines)
            .WithOne()
            .HasForeignKey(x => x.BillId)
            .OnDelete(DeleteBehavior.Cascade);

        builder.Navigation(x => x.Lines)
            .UsePropertyAccessMode(PropertyAccessMode.Field);
    }
}
