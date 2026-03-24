package com.calculator.domain.dto.tactics;

import com.calculator.domain.dto.tactics.security.SecurityTactics;

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
        int sagaPivotTransactions,
        SecurityTactics securityTactics
) { }
