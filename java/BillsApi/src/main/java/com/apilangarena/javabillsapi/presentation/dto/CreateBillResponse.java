package com.apilangarena.javabillsapi.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBillResponse(
    Long id,
    String billNumber,
    LocalDate issuedAt,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal total,
    String currency
) {
}
