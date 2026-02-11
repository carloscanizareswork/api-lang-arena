package com.apilangarena.javabillsapi.domain.common;

import java.util.List;
import java.util.Map;

public class DomainValidationException extends RuntimeException {
    private final Map<String, List<String>> errors;

    public DomainValidationException(Map<String, List<String>> errors) {
        super("Domain validation failed");
        this.errors = errors;
    }

    public Map<String, List<String>> getErrors() {
        return errors;
    }
}
