package com.apilangarena.billsapi.application.bills.get

import java.math.BigDecimal
import java.time.LocalDate

data class BillSummaryDto(
    val id: Long,
    val billNumber: String,
    val issuedAt: LocalDate,
    val total: BigDecimal,
    val currency: String,
)
