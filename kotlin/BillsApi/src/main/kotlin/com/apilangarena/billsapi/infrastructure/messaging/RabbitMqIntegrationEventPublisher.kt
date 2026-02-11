package com.apilangarena.billsapi.infrastructure.messaging

import com.apilangarena.billsapi.application.bills.create.BillCreatedEvent
import com.apilangarena.billsapi.application.bills.create.IntegrationEventPublisher
import org.springframework.amqp.core.MessageDeliveryMode
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class RabbitMqIntegrationEventPublisher(
    private val rabbitTemplate: RabbitTemplate,
    private val messagingProperties: MessagingProperties,
) : IntegrationEventPublisher {
    override fun publishBillCreated(event: BillCreatedEvent) {
        val envelope = IntegrationEnvelope(
            eventName = "bill.created",
            occurredAtUtc = OffsetDateTime.now(ZoneOffset.UTC),
            payload = event,
        )

        rabbitTemplate.convertAndSend("", messagingProperties.billCreatedQueue, envelope) { message ->
            message.messageProperties.contentType = "application/json"
            message.messageProperties.deliveryMode = MessageDeliveryMode.PERSISTENT
            message
        }
    }
}

data class IntegrationEnvelope<TPayload>(
    val eventName: String,
    val occurredAtUtc: OffsetDateTime,
    val payload: TPayload,
)
