package com.apilangarena.javabillsapi.application.bills.create;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBillResult(
    Long id,
    String billNumber,
    LocalDate issuedAt,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal total,
    String currency
) {
}
