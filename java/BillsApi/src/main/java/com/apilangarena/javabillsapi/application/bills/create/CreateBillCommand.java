package com.apilangarena.javabillsapi.application.bills.create;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateBillCommand(
    String billNumber,
    LocalDate issuedAt,
    String customerName,
    String currency,
    BigDecimal tax,
    List<CreateBillLineCommand> lines
) {
}
