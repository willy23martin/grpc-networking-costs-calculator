package com.calculator.domain.model.results;

public record MessageSizeCalculationResult(
        Class<?> messageClass,
        int size
) { }
