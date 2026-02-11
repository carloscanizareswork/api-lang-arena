package com.apilangarena.javabillsapi.application.bills.create;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BillCreatedEvent(
    Long billId,
    String billNumber,
    LocalDate issuedAt,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal total,
    String currency,
    OffsetDateTime occurredAtUtc,
    String source
) {
}
