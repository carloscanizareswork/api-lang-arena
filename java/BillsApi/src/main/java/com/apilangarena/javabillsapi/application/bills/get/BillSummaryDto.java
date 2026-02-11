package com.apilangarena.javabillsapi.application.bills.get;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillSummaryDto(
    Long id,
    String billNumber,
    LocalDate issuedAt,
    BigDecimal total,
    String currency
) {
}
