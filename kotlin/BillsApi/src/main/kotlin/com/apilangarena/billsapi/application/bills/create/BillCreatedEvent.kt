package com.apilangarena.billsapi.application.bills.create

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class BillCreatedEvent(
    val billId: Long,
    val billNumber: String,
    val issuedAt: LocalDate,
    val subtotal: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val currency: String,
    val occurredAtUtc: OffsetDateTime,
    val source: String,
)
