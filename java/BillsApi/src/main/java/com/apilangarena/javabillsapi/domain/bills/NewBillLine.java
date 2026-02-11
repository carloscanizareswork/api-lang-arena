package com.apilangarena.javabillsapi.domain.bills;

import com.apilangarena.javabillsapi.domain.common.DomainValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class NewBillLine {
    private final int lineNo;
    private final String concept;
    private final BigDecimal quantity;
    private final BigDecimal unitAmount;
    private final BigDecimal lineAmount;

    private NewBillLine(int lineNo, String concept, BigDecimal quantity, BigDecimal unitAmount, BigDecimal lineAmount) {
        this.lineNo = lineNo;
        this.concept = concept;
        this.quantity = quantity;
        this.unitAmount = unitAmount;
        this.lineAmount = lineAmount;
    }

    public static NewBillLine create(int lineNo, String concept, BigDecimal quantity, BigDecimal unitAmount) {
        Map<String, List<String>> errors = new LinkedHashMap<>();

        if (lineNo <= 0) {
            addError(errors, "lines.lineNo", "Line number must be greater than zero.");
        }
        if (concept == null || concept.isBlank()) {
            addError(errors, "lines.concept", "Line concept is required.");
        } else if (concept.length() > 200) {
            addError(errors, "lines.concept", "Line concept max length is 200.");
        }
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            addError(errors, "lines.quantity", "Line quantity must be greater than zero.");
        }
        if (unitAmount == null || unitAmount.compareTo(BigDecimal.ZERO) < 0) {
            addError(errors, "lines.unitAmount", "Line unit amount cannot be negative.");
        }

        if (!errors.isEmpty()) {
            throw new DomainValidationException(errors);
        }

        BigDecimal normalizedQty = money(quantity);
        BigDecimal normalizedUnit = money(unitAmount);
        BigDecimal normalizedLineAmount = money(normalizedQty.multiply(normalizedUnit));

        return new NewBillLine(
            lineNo,
            concept.trim(),
            normalizedQty,
            normalizedUnit,
            normalizedLineAmount
        );
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private static void addError(Map<String, List<String>> errors, String field, String message) {
        errors.computeIfAbsent(field, ignored -> new ArrayList<>()).add(message);
    }

    public int getLineNo() {
        return lineNo;
    }

    public String getConcept() {
        return concept;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitAmount() {
        return unitAmount;
    }

    public BigDecimal getLineAmount() {
        return lineAmount;
    }
}
