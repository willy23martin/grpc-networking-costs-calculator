package com.calculator.domain.model.tactics.resiliency;

public record CircuitBreakerTactic(
        boolean resiliencyCircuitBreakerPattern,
        int circuitBreakerPatternMinimumCalls,
        int circuitBreakerHalfOpen,
        int circuitBreakerWaitMilliseconds,
        int circuitBreakerFailureRate
) {
}
