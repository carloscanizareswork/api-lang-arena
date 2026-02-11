package com.apilangarena.billsapi.presentation.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateBillLineRequest(
    @field:NotBlank(message = "Line concept is required.")
    @field:Size(max = 200, message = "Line concept max length is 200.")
    val concept: String,

    @field:DecimalMin(value = "0.01", inclusive = true, message = "Line quantity must be greater than zero.")
    val quantity: BigDecimal,

    @field:DecimalMin(value = "0.00", inclusive = true, message = "Line unit amount cannot be negative.")
    val unitAmount: BigDecimal,
)

data class CreateBillRequest(
    @field:NotBlank(message = "Bill number is required.")
    @field:Size(max = 50, message = "Bill number max length is 50.")
    val billNumber: String,

    @field:NotNull(message = "Issued date is required.")
    val issuedAt: LocalDate,

    @field:NotBlank(message = "Customer name is required.")
    @field:Size(max = 200, message = "Customer name max length is 200.")
    val customerName: String,

    @field:NotBlank(message = "Currency is required.")
    @field:Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code.")
    val currency: String,

    @field:DecimalMin(value = "0.00", inclusive = true, message = "Tax cannot be negative.")
    val tax: BigDecimal,

    @field:NotEmpty(message = "At least one line is required.")
    @field:Valid
    val lines: List<CreateBillLineRequest>,
)
