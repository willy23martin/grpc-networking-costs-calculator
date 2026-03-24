package com.calculator.domain.dto.results;

public record MessageSizeCalculationResult(
        Class<?> messageClass,
        int size
) { }
