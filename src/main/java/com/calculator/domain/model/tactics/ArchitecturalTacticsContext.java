package com.calculator.domain.model.tactics;

public record ArchitecturalTacticsContext(
        boolean reliabilityClientSideLoadBalancerTactic,
        boolean reliabilityServerSideLoadBalancerTactic,
        boolean resiliencyTimeoutTactic,
        long tacticTimeoutMilliseconds,
        boolean resiliencyRetryTactic,
        int tacticRetryTimes,
        boolean resiliencyCircuitBreakerPattern,
        int circuitBreakerPatternMinimumCalls,
        int circuitBreakerHalfOpen,
        long circuitBreakerWaitMilliseconds,
        int circuitBreakerFailureRate,
        boolean microservicesSAGAPattern,
        int sagaCompensatableTransactions,
        int sagaRetriableTransactions,
        int sagaPivotTransactions
) { }
