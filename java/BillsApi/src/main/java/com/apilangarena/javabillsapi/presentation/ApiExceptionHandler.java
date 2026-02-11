package com.apilangarena.javabillsapi.presentation;

import com.apilangarena.javabillsapi.application.common.ConflictException;
import com.apilangarena.javabillsapi.domain.common.DomainValidationException;
import org.springframework.amqp.AmqpException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();
        for (var error : ex.getBindingResult().getAllErrors()) {
            String fieldName = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            String message = error.getDefaultMessage() == null ? "Invalid value" : error.getDefaultMessage();
            fieldErrors.computeIfAbsent(fieldName, ignored -> new ArrayList<>()).add(message);
        }

        return problem(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemResponse> handleInvalidJson(HttpMessageNotReadableException ignored) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", Map.of("request", List.of("Invalid JSON payload.")));
    }

    @ExceptionHandler(DomainValidationException.class)
    public ResponseEntity<ProblemResponse> handleDomainValidation(DomainValidationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", ex.getErrors());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ProblemResponse> handleConflict(ConflictException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(AmqpException.class)
    public ResponseEntity<ProblemResponse> handleAmqp(AmqpException ex) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Message broker error: " + ex.getClass().getSimpleName(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemResponse> handleUnhandled(Exception ignored) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", null);
    }

    private ResponseEntity<ProblemResponse> problem(HttpStatus status, String title, Map<String, List<String>> errors) {
        return ResponseEntity
            .status(status)
            .body(new ProblemResponse("about:blank", title, status.value(), errors));
    }
}
