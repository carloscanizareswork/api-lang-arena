package com.apilangarena.billsapi.infrastructure.messaging

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.messaging")
data class MessagingProperties(
    var billCreatedQueue: String = "bill-created",
)
