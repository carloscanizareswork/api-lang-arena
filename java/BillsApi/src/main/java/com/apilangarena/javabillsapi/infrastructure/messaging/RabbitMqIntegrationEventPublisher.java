package com.apilangarena.javabillsapi.infrastructure.messaging;

import com.apilangarena.javabillsapi.application.bills.create.BillCreatedEvent;
import com.apilangarena.javabillsapi.application.bills.create.IntegrationEventPublisher;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class RabbitMqIntegrationEventPublisher implements IntegrationEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties messagingProperties;

    public RabbitMqIntegrationEventPublisher(RabbitTemplate rabbitTemplate, MessagingProperties messagingProperties) {
        this.rabbitTemplate = rabbitTemplate;
        this.messagingProperties = messagingProperties;
    }

    @Override
    public void publishBillCreated(BillCreatedEvent event) {
        IntegrationEnvelope<BillCreatedEvent> envelope = new IntegrationEnvelope<>(
            "bill.created",
            OffsetDateTime.now(ZoneOffset.UTC),
            event
        );

        rabbitTemplate.convertAndSend("", messagingProperties.getBillCreatedQueue(), envelope, message -> {
            message.getMessageProperties().setContentType("application/json");
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            return message;
        });
    }
}
