using System.Text.Json;
using BillsApi.Application.Abstractions.Messaging;
using RabbitMQ.Client;

namespace BillsApi.Infrastructure.Messaging;

public sealed class RabbitMqIntegrationEventPublisher : IIntegrationEventPublisher, IDisposable
{
    private readonly IConnection _connection;
    private readonly RabbitMqOptions _options;

    public RabbitMqIntegrationEventPublisher(RabbitMqOptions options)
    {
        _options = options;

        var factory = new ConnectionFactory
        {
            HostName = options.HostName,
            Port = options.Port,
            UserName = options.UserName,
            Password = options.Password,
            VirtualHost = options.VirtualHost,
            AutomaticRecoveryEnabled = true,
            TopologyRecoveryEnabled = true
        };

        _connection = factory.CreateConnection();
    }

    public Task PublishAsync<TMessage>(string eventName, TMessage message, CancellationToken cancellationToken = default)
    {
        cancellationToken.ThrowIfCancellationRequested();

        using var channel = _connection.CreateModel();
        channel.QueueDeclare(
            queue: _options.BillCreatedQueueName,
            durable: true,
            exclusive: false,
            autoDelete: false,
            arguments: null);

        var envelope = new IntegrationEnvelope<TMessage>(eventName, DateTime.UtcNow, message);
        var body = JsonSerializer.SerializeToUtf8Bytes(envelope);

        var properties = channel.CreateBasicProperties();
        properties.Persistent = true;
        properties.ContentType = "application/json";

        channel.BasicPublish(
            exchange: string.Empty,
            routingKey: _options.BillCreatedQueueName,
            basicProperties: properties,
            body: body);

        return Task.CompletedTask;
    }

    public void Dispose()
    {
        _connection.Dispose();
    }
}

file sealed record IntegrationEnvelope<TPayload>(string EventName, DateTime OccurredAtUtc, TPayload Payload);
