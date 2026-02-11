using BillsApi.Application.Bills.CreateBill;
using BillsApi.Application.Bills.GetBills;
using BillsApi.Presentation.Contracts;
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
                SELECT b.id,
                       b.bill_number,
                       b.issued_at,
                       COALESCE(SUM(bl.line_amount), 0) + b.tax AS total,
                       b.currency
                FROM bill b
                LEFT JOIN bill_line bl ON bl.bill_id = b.id
                GROUP BY b.id, b.bill_number, b.issued_at, b.tax, b.currency
                ORDER BY b.id;
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

        app.MapPost("/bills", async (CreateBillRequest request, ISender sender, CancellationToken ct) =>
        {
            var command = new CreateBillCommand(
                request.BillNumber,
                request.IssuedAt,
                request.CustomerName,
                request.Currency,
                request.Tax,
                request.Lines.Select(x => new CreateBillLineInput(x.Concept, x.Quantity, x.UnitAmount)).ToList());

            var result = await sender.Send(command, ct);
            return Results.Created($"/bills/{result.Id}", result);
        });

        return app;
    }
}
