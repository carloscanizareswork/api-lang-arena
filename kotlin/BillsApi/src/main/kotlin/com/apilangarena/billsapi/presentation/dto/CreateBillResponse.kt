package com.apilangarena.billsapi.presentation.dto

import java.math.BigDecimal
import java.time.LocalDate

data class CreateBillResponse(
    val id: Long,
    val billNumber: String,
    val issuedAt: LocalDate,
    val subtotal: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val currency: String,
)
