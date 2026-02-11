package com.apilangarena.javabillsapi.infrastructure.messaging;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.messaging")
public class MessagingProperties {
    private String billCreatedQueue = "bill-created";

    public String getBillCreatedQueue() {
        return billCreatedQueue;
    }

    public void setBillCreatedQueue(String billCreatedQueue) {
        this.billCreatedQueue = billCreatedQueue;
    }
}
