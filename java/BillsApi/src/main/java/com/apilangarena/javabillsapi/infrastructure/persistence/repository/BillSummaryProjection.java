package com.apilangarena.javabillsapi.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BillSummaryProjection {
    Long getId();

    String getBillNumber();

    LocalDate getIssuedAt();

    BigDecimal getTotal();

    String getCurrency();
}
