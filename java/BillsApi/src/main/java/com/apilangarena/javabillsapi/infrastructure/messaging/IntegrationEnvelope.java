package com.apilangarena.javabillsapi.infrastructure.messaging;

import java.time.OffsetDateTime;

public record IntegrationEnvelope<TPayload>(
    String eventName,
    OffsetDateTime occurredAtUtc,
    TPayload payload
) {
}
