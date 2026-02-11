package com.apilangarena.javabillsapi.presentation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateBillLineRequest(
    @NotBlank(message = "Line concept is required.")
    @Size(max = 200, message = "Line concept max length is 200.")
    String concept,

    @DecimalMin(value = "0.01", inclusive = true, message = "Line quantity must be greater than zero.")
    BigDecimal quantity,

    @DecimalMin(value = "0.00", inclusive = true, message = "Line unit amount cannot be negative.")
    BigDecimal unitAmount
) {
}
