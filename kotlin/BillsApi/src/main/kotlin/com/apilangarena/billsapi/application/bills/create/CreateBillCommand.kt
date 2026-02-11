package com.apilangarena.billsapi.application.bills.create

import java.math.BigDecimal
import java.time.LocalDate

data class CreateBillLineCommand(
    val concept: String,
    val quantity: BigDecimal,
    val unitAmount: BigDecimal,
)

data class CreateBillCommand(
    val billNumber: String,
    val issuedAt: LocalDate,
    val customerName: String,
    val currency: String,
    val tax: BigDecimal,
    val lines: List<CreateBillLineCommand>,
)
