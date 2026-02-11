package com.apilangarena.billsapi.domain.bills

import com.apilangarena.billsapi.domain.common.DomainValidationException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private fun BigDecimal.money(): BigDecimal = this.setScale(2, RoundingMode.HALF_UP)

data class NewBillLine(
    val lineNo: Int,
    val concept: String,
    val quantity: BigDecimal,
    val unitAmount: BigDecimal,
    val lineAmount: BigDecimal,
) {
    companion object {
        fun create(lineNo: Int, concept: String, quantity: BigDecimal, unitAmount: BigDecimal): NewBillLine {
            val lineErrors = mutableMapOf<String, MutableList<String>>()
            if (lineNo <= 0) lineErrors.getOrPut("lines.lineNo") { mutableListOf() }.add("Line number must be greater than zero.")
            if (concept.isBlank()) lineErrors.getOrPut("lines.concept") { mutableListOf() }.add("Line concept is required.")
            if (concept.length > 200) lineErrors.getOrPut("lines.concept") { mutableListOf() }.add("Line concept max length is 200.")
            if (quantity <= BigDecimal.ZERO) lineErrors.getOrPut("lines.quantity") { mutableListOf() }.add("Line quantity must be greater than zero.")
            if (unitAmount < BigDecimal.ZERO) lineErrors.getOrPut("lines.unitAmount") { mutableListOf() }.add("Line unit amount cannot be negative.")
            if (lineErrors.isNotEmpty()) {
                throw DomainValidationException(lineErrors.mapValues { it.value.toList() })
            }

            val normalizedQty = quantity.money()
            val normalizedUnit = unitAmount.money()
            return NewBillLine(
                lineNo = lineNo,
                concept = concept.trim(),
                quantity = normalizedQty,
                unitAmount = normalizedUnit,
                lineAmount = (normalizedQty.multiply(normalizedUnit)).money(),
            )
        }
    }
}

data class NewBill(
    val billNumber: String,
    val issuedAt: LocalDate,
    val customerName: String,
    val currency: String,
    val tax: BigDecimal,
    val lines: List<NewBillLine>,
) {
    val subtotal: BigDecimal = lines.fold(BigDecimal.ZERO) { acc, line -> acc + line.lineAmount }.money()
    val total: BigDecimal = (subtotal + tax).money()

    companion object {
        fun create(
            billNumber: String,
            issuedAt: LocalDate,
            customerName: String,
            currency: String,
            tax: BigDecimal,
            lines: List<NewBillLine>,
        ): NewBill {
            val normalizedBillNumber = billNumber.trim()
            val normalizedCustomerName = customerName.trim()
            val normalizedCurrency = currency.trim().uppercase()
            val normalizedTax = tax.money()

            val errors = mutableMapOf<String, MutableList<String>>()
            if (normalizedBillNumber.isBlank()) errors.getOrPut("billNumber") { mutableListOf() }.add("Bill number is required.")
            if (normalizedBillNumber.length > 50) errors.getOrPut("billNumber") { mutableListOf() }.add("Bill number max length is 50.")
            if (normalizedCustomerName.isBlank()) errors.getOrPut("customerName") { mutableListOf() }.add("Customer name is required.")
            if (normalizedCustomerName.length > 200) errors.getOrPut("customerName") { mutableListOf() }.add("Customer name max length is 200.")
            if (normalizedCurrency.length != 3) errors.getOrPut("currency") { mutableListOf() }.add("Currency must be a 3-letter ISO code.")
            if (tax < BigDecimal.ZERO) errors.getOrPut("tax") { mutableListOf() }.add("Tax cannot be negative.")
            if (lines.isEmpty()) errors.getOrPut("lines") { mutableListOf() }.add("At least one line is required.")

            if (errors.isNotEmpty()) {
                throw DomainValidationException(errors.mapValues { it.value.toList() })
            }

            return NewBill(
                billNumber = normalizedBillNumber,
                issuedAt = issuedAt,
                customerName = normalizedCustomerName,
                currency = normalizedCurrency,
                tax = normalizedTax,
                lines = lines,
            )
        }
    }
}
