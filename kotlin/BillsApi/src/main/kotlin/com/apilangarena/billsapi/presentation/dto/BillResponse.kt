package com.apilangarena.billsapi.presentation.dto

import java.math.BigDecimal
import java.time.LocalDate

data class BillResponse(
    val id: Long,
    val billNumber: String,
    val issuedAt: LocalDate,
    val total: BigDecimal,
    val currency: String,
)
