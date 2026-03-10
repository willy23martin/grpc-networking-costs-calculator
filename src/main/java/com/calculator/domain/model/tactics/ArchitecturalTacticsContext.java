package com.calculator.domain.model.tactics;

public record ArchitecturalTacticsContext(
        boolean reliabilityClientSideLoadBalancerTactic,
        boolean reliabilityServerSideLoadBalancerTactic,
        boolean resiliencyTimeoutTactic,
        int tacticTimeoutMilliseconds,
        boolean resiliencyRetryTactic,
        int     tacticRetryTimes,
        boolean resiliencyCircuitBreakerPattern,
        int circuitBreakerPatternMinimumCalls,
        int circuitBreakerHalfOpen,
        int circuitBreakerWaitMilliseconds,
        int circuitBreakerFailureRate,
        boolean microservicesSAGAPattern,
        int sagaCompensatableTransactions,
        int sagaRetriableTransactions,
        int sagaPivotTransactions
) { }
