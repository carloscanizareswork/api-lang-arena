package com.apilangarena.javabillsapi.application.bills.create;

import java.math.BigDecimal;

public record CreateBillLineCommand(
    String concept,
    BigDecimal quantity,
    BigDecimal unitAmount
) {
}
