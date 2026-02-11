package com.apilangarena.billsapi.infrastructure.persistence.repository

import java.math.BigDecimal
import java.time.LocalDate

interface BillSummaryProjection {
    val id: Long
    val billNumber: String
    val issuedAt: LocalDate
    val total: BigDecimal
    val currency: String
}
