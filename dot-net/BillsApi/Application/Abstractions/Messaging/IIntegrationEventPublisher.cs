namespace BillsApi.Application.Abstractions.Messaging;

public interface IIntegrationEventPublisher
{
    Task PublishAsync<TMessage>(string eventName, TMessage message, CancellationToken cancellationToken = default);
}
