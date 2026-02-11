using BillsApi.Application.Bills.GetBills;
using BillsApi.Domain.Bills;
using BillsApi.Infrastructure.Persistence;
using BillsApi.Presentation.Endpoints;
using Microsoft.EntityFrameworkCore;
using Npgsql;

var builder = WebApplication.CreateBuilder(args);

var connectionString = BuildConnectionString(builder.Configuration);

builder.Services.AddSingleton(_ => NpgsqlDataSource.Create(connectionString));
builder.Services.AddDbContext<ArenaDbContext>(options => options.UseNpgsql(connectionString));

builder.Services.AddScoped<IBillRepository, BillRepository>();

builder.Services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(GetBillsQuery).Assembly));

var app = builder.Build();

app.MapGet("/", () => Results.Ok(new { service = "dot-net-bills-api", status = "ok" }));
app.MapBillsEndpoints();

app.Run();

static string BuildConnectionString(IConfiguration configuration)
{
    var host = configuration["POSTGRES_HOST"] ?? "localhost";
    var port = configuration["POSTGRES_PORT"] ?? "5440";
    var db = configuration["POSTGRES_DB"] ?? "api_lang_arena";
    var user = configuration["POSTGRES_USER"] ?? "api_lang_user";
    var password = configuration["POSTGRES_PASSWORD"] ?? "api_lang_password";

    return $"Host={host};Port={port};Database={db};Username={user};Password={password}";
}
