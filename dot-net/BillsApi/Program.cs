using BillsApi.Application.Abstractions.Messaging;
using BillsApi.Application.Abstractions.Validation;
using BillsApi.Application.Bills.GetBills;
using BillsApi.Domain.Bills;
using BillsApi.Domain.Common;
using BillsApi.Infrastructure.Messaging;
using BillsApi.Infrastructure.Persistence;
using BillsApi.Presentation.Endpoints;
using BillsApi.Presentation.Middleware;
using FluentValidation;
using MediatR;
using Microsoft.EntityFrameworkCore;
using Npgsql;

var builder = WebApplication.CreateBuilder(args);

var connectionString = BuildConnectionString(builder.Configuration);

builder.Services.AddSingleton(_ => new NpgsqlDataSourceBuilder(connectionString).Build());
builder.Services.AddDbContext<ArenaDbContext>((sp, options) =>
    options.UseNpgsql(sp.GetRequiredService<NpgsqlDataSource>()));

builder.Services.AddScoped<IBillRepository, BillRepository>();
builder.Services.AddScoped<IGetBillsReadRepository, GetBillsReadRepository>();
builder.Services.AddScoped<IUnitOfWork>(sp => sp.GetRequiredService<ArenaDbContext>());

builder.Services.AddMediatR(cfg => cfg.RegisterServicesFromAssembly(typeof(GetBillsQuery).Assembly));
builder.Services.AddValidatorsFromAssembly(typeof(GetBillsQuery).Assembly);
builder.Services.AddTransient(typeof(IPipelineBehavior<,>), typeof(ValidationBehavior<,>));

builder.Services.AddSingleton(BuildRabbitMqOptions(builder.Configuration));
builder.Services.AddSingleton<IIntegrationEventPublisher, RabbitMqIntegrationEventPublisher>();

var app = builder.Build();

app.UseMiddleware<ExceptionHandlingMiddleware>();

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
    var poolMinSize = configuration["DOTNET_DB_POOL_MIN_SIZE"] ?? "1";
    var poolMaxSize = configuration["DOTNET_DB_POOL_MAX_SIZE"] ?? "8";

    return $"Host={host};Port={port};Database={db};Username={user};Password={password};Minimum Pool Size={poolMinSize};Maximum Pool Size={poolMaxSize}";
}

static RabbitMqOptions BuildRabbitMqOptions(IConfiguration configuration)
{
    return new RabbitMqOptions
    {
        HostName = configuration["RABBITMQ_HOST"] ?? "localhost",
        Port = int.TryParse(configuration["RABBITMQ_PORT"], out var port) ? port : 5672,
        UserName = configuration["RABBITMQ_USER"] ?? "guest",
        Password = configuration["RABBITMQ_PASSWORD"] ?? "guest",
        VirtualHost = configuration["RABBITMQ_VHOST"] ?? "/",
        BillCreatedQueueName = configuration["RABBITMQ_BILL_CREATED_QUEUE"] ?? "bill-created"
    };
}
