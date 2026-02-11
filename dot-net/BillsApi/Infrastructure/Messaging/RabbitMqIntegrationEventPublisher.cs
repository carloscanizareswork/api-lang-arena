using System.Text.Json;
using BillsApi.Application.Abstractions.Messaging;
using RabbitMQ.Client;

namespace BillsApi.Infrastructure.Messaging;

public sealed class RabbitMqIntegrationEventPublisher : IIntegrationEventPublisher, IDisposable
{
    private readonly RabbitMqOptions _options;
    private readonly object _sync = new();
    private IConnection? _connection;
    private IModel? _channel;

    public RabbitMqIntegrationEventPublisher(RabbitMqOptions options)
    {
        _options = options;
        EnsureChannel();
    }

    public Task PublishAsync<TMessage>(string eventName, TMessage message, CancellationToken cancellationToken = default)
    {
        cancellationToken.ThrowIfCancellationRequested();

        var envelope = new IntegrationEnvelope<TMessage>(eventName, DateTime.UtcNow, message);
        var body = JsonSerializer.SerializeToUtf8Bytes(envelope);

        lock (_sync)
        {
            EnsureChannel();

            var channel = _channel!;
            var properties = channel.CreateBasicProperties();
            properties.Persistent = true;
            properties.ContentType = "application/json";

            channel.BasicPublish(
                exchange: string.Empty,
                routingKey: _options.BillCreatedQueueName,
                basicProperties: properties,
                body: body);
        }

        return Task.CompletedTask;
    }

    public void Dispose()
    {
        lock (_sync)
        {
            _channel?.Dispose();
            _connection?.Dispose();
            _channel = null;
            _connection = null;
        }
    }

    private void EnsureChannel()
    {
        if (_connection is { IsOpen: true } && _channel is { IsOpen: true })
        {
            return;
        }

        _channel?.Dispose();
        _connection?.Dispose();

        var factory = BuildFactory(_options);
        _connection = factory.CreateConnection();
        _channel = _connection.CreateModel();
        _channel.QueueDeclare(
            queue: _options.BillCreatedQueueName,
            durable: true,
            exclusive: false,
            autoDelete: false,
            arguments: null);
    }

    private static ConnectionFactory BuildFactory(RabbitMqOptions options)
    {
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

        return factory;
    }
}

file sealed record IntegrationEnvelope<TPayload>(string EventName, DateTime OccurredAtUtc, TPayload Payload);
