package com.apilangarena.javabillsapi.presentation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateBillRequest(
    @NotBlank(message = "Bill number is required.")
    @Size(max = 50, message = "Bill number max length is 50.")
    String billNumber,

    @NotNull(message = "Issued date is required.")
    LocalDate issuedAt,

    @NotBlank(message = "Customer name is required.")
    @Size(max = 200, message = "Customer name max length is 200.")
    String customerName,

    @NotBlank(message = "Currency is required.")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code.")
    String currency,

    @DecimalMin(value = "0.00", inclusive = true, message = "Tax cannot be negative.")
    BigDecimal tax,

    @NotEmpty(message = "At least one line is required.")
    @Valid
    List<CreateBillLineRequest> lines
) {
}
