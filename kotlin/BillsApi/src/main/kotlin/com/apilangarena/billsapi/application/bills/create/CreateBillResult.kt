package com.apilangarena.billsapi.application.bills.create

import java.math.BigDecimal
import java.time.LocalDate

data class CreateBillResult(
    val id: Long,
    val billNumber: String,
    val issuedAt: LocalDate,
    val subtotal: BigDecimal,
    val tax: BigDecimal,
    val total: BigDecimal,
    val currency: String,
)
