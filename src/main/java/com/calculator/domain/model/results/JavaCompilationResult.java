package com.calculator.domain.model.results;

public record JavaCompilationResult(
        String errorCompilationMessage,
        CompilationResult compilationResult
) {
}
