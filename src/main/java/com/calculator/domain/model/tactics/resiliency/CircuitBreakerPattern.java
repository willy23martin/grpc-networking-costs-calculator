package com.calculator.domain.model.tactics.resiliency;

public record CircuitBreakerPattern(
        boolean resiliencyCircuitBreakerPattern,
        int circuitBreakerPatternMinimumCalls,
        int circuitBreakerHalfOpen,
        long circuitBreakerWaitMilliseconds,
        int circuitBreakerFailureRate
) { }