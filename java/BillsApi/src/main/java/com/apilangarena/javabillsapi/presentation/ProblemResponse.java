package com.apilangarena.javabillsapi.presentation;

import java.util.List;
import java.util.Map;

public record ProblemResponse(
    String type,
    String title,
    Integer status,
    Map<String, List<String>> errors
) {
}
