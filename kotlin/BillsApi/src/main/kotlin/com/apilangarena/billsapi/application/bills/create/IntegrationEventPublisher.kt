package com.apilangarena.billsapi.application.bills.create

interface IntegrationEventPublisher {
    fun publishBillCreated(event: BillCreatedEvent)
}
