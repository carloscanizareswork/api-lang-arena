package com.apilangarena.javabillsapi.presentation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillResponse(
    Long id,
    String billNumber,
    LocalDate issuedAt,
    BigDecimal total,
    String currency
) {
}
