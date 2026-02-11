using BillsApi.Application.Bills.GetBills;
using MediatR;
using Npgsql;

namespace BillsApi.Presentation.Endpoints;

public static class BillsEndpoints
{
    public static IEndpointRouteBuilder MapBillsEndpoints(this IEndpointRouteBuilder app)
    {
        app.MapGet("/bills-minimal", async (NpgsqlDataSource dataSource, CancellationToken ct) =>
        {
            const string sql = """
                SELECT id, bill_number, issued_at, total, currency
                FROM bill
                ORDER BY id;
                """;

            var bills = new List<BillDto>();

            await using var cmd = dataSource.CreateCommand(sql);
            await using var reader = await cmd.ExecuteReaderAsync(ct);

            while (await reader.ReadAsync(ct))
            {
                bills.Add(new BillDto(
                    reader.GetInt64(0),
                    reader.GetString(1),
                    reader.GetFieldValue<DateOnly>(2),
                    reader.GetDecimal(3),
                    reader.GetString(4)
                ));
            }

            return Results.Ok(bills);
        });

        app.MapGet("/bills", async (ISender sender, CancellationToken ct) =>
        {
            var result = await sender.Send(new GetBillsQuery(), ct);
            return Results.Ok(result);
        });

        return app;
    }
}
