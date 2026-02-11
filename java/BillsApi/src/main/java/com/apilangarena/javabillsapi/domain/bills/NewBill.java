package com.apilangarena.javabillsapi.domain.bills;

import com.apilangarena.javabillsapi.domain.common.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NewBill {
    private final String billNumber;
    private final LocalDate issuedAt;
    private final String customerName;
    private final String currency;
    private final BigDecimal tax;
    private final List<NewBillLine> lines;
    private final BigDecimal subtotal;
    private final BigDecimal total;

    private NewBill(
        String billNumber,
        LocalDate issuedAt,
        String customerName,
        String currency,
        BigDecimal tax,
        List<NewBillLine> lines
    ) {
        this.billNumber = billNumber;
        this.issuedAt = issuedAt;
        this.customerName = customerName;
        this.currency = currency;
        this.tax = tax;
        this.lines = List.copyOf(lines);
        this.subtotal = money(lines.stream().map(NewBillLine::getLineAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        this.total = money(this.subtotal.add(this.tax));
    }

    public static NewBill create(
        String billNumber,
        LocalDate issuedAt,
        String customerName,
        String currency,
        BigDecimal tax,
        List<NewBillLine> lines
    ) {
        String normalizedBillNumber = billNumber == null ? "" : billNumber.trim();
        String normalizedCustomerName = customerName == null ? "" : customerName.trim();
        String normalizedCurrency = currency == null ? "" : currency.trim().toUpperCase();
        BigDecimal normalizedTax = money(tax == null ? BigDecimal.ZERO : tax);

        Map<String, List<String>> errors = new LinkedHashMap<>();

        if (normalizedBillNumber.isBlank()) {
            addError(errors, "billNumber", "Bill number is required.");
        } else if (normalizedBillNumber.length() > 50) {
            addError(errors, "billNumber", "Bill number max length is 50.");
        }

        if (normalizedCustomerName.isBlank()) {
            addError(errors, "customerName", "Customer name is required.");
        } else if (normalizedCustomerName.length() > 200) {
            addError(errors, "customerName", "Customer name max length is 200.");
        }

        if (issuedAt == null) {
            addError(errors, "issuedAt", "Issued date is required.");
        }

        if (normalizedCurrency.length() != 3) {
            addError(errors, "currency", "Currency must be a 3-letter ISO code.");
        }

        if (tax == null || tax.compareTo(BigDecimal.ZERO) < 0) {
            addError(errors, "tax", "Tax cannot be negative.");
        }

        if (lines == null || lines.isEmpty()) {
            addError(errors, "lines", "At least one line is required.");
        }

        if (!errors.isEmpty()) {
            throw new DomainValidationException(errors);
        }

        return new NewBill(
            normalizedBillNumber,
            issuedAt,
            normalizedCustomerName,
            normalizedCurrency,
            normalizedTax,
            lines
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static void addError(Map<String, List<String>> errors, String field, String message) {
        errors.computeIfAbsent(field, ignored -> new ArrayList<>()).add(message);
    }

    public String getBillNumber() {
        return billNumber;
    }

    public LocalDate getIssuedAt() {
        return issuedAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getTax() {
        return tax;
    }

    public List<NewBillLine> getLines() {
        return lines;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
