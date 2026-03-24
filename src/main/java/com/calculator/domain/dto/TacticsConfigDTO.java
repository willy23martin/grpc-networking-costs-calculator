package com.calculator.domain.dto;

import com.calculator.domain.dto.tactics.ArchitecturalTacticsContext;
import com.calculator.domain.dto.tactics.microservices.SAGAPattern;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;

public record TacticsConfigDTO(
        long requestsPerSecond,
        ReliabilityTactics reliabilityTactics,
        TimeoutPattern timeoutTactic,
        RetryPattern retryTactic,
        CircuitBreakerPattern circuitBreakerTactic,
        SAGAPattern sagaPattern,
        SecurityTactics securityTactics
) {
    public static TacticsConfigDTO empty() {
        return new TacticsConfigDTO(
                0,
                ReliabilityTactics.empty(),
                TimeoutPattern.empty(),
                RetryPattern.empty(),
                CircuitBreakerPattern.empty(),
                SAGAPattern.empty(),
                SecurityTactics.empty()
        );
    }

    public ArchitecturalTacticsContext toTacticsContext() {
        return new ArchitecturalTacticsContext(
                reliabilityTactics.reliabilityClientSideLoadBalancerTactic(),
                reliabilityTactics.reliabilityServerSideLoadBalancerTactic(),
                timeoutTactic.resiliencyTimeoutTactic(),
                timeoutTactic.tacticTimeoutMilliseconds(),
                retryTactic.resiliencyRetryTactic(),
                retryTactic.tacticRetryTimes(),
                circuitBreakerTactic.resiliencyCircuitBreakerPattern(),
                circuitBreakerTactic.circuitBreakerPatternMinimumCalls(),
                circuitBreakerTactic.circuitBreakerHalfOpen(),
                circuitBreakerTactic.circuitBreakerWaitMilliseconds(),
                circuitBreakerTactic.circuitBreakerFailureRate(),
                sagaPattern.microservicesSAGAPattern(),
                sagaPattern.sagaCompensatableTransactions(),
                sagaPattern.sagaRetriableTransactions(),
                sagaPattern.sagaPivotTransactions(),
                securityTactics
        );
    }
}