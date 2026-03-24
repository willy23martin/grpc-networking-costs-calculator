package com.calculator.domain.dto.tactics.resiliency;

public record CircuitBreakerPattern(
        boolean resiliencyCircuitBreakerPattern,
        int circuitBreakerPatternMinimumCalls,
        int circuitBreakerHalfOpen,
        long circuitBreakerWaitMilliseconds,
        int circuitBreakerFailureRate
) {
    public static CircuitBreakerPattern empty(){
        return new CircuitBreakerPattern(false, 0, 0, 0, 0);
    }
}