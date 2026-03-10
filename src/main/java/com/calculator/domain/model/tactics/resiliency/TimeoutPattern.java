package com.calculator.domain.model.tactics.resiliency;

public record TimeoutPattern(
        boolean resiliencyTimeoutTactic,
        long tacticTimeoutMilliseconds
) { }