package com.calculator.domain.dto.results;

public record JavaCompilationResult(
        String errorCompilationMessage,
        CompilationResult compilationResult
) {
}
