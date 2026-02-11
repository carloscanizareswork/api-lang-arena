package com.apilangarena.javabillsapi.application.bills.create;

public interface IntegrationEventPublisher {
    void publishBillCreated(BillCreatedEvent event);
}
