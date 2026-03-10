package com.calculator.domain.model.tactics;

import com.calculator.domain.model.tactics.microservices.SAGAPattern;
import com.calculator.domain.model.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.model.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.model.tactics.resiliency.RetryPattern;
import com.calculator.domain.model.tactics.resiliency.TimeoutPattern;

public record TacticsConfigDTO(
        long requestsPerSecond,
        ReliabilityTactics reliabilityTactics,
        TimeoutPattern timeoutTactic,
        RetryPattern retryTactic,
        CircuitBreakerPattern circuitBreakerTactic,
        SAGAPattern sagaPattern
) {
    public static TacticsConfigDTO empty() {
        return new TacticsConfigDTO(
                0,
                new ReliabilityTactics(false, false),
                new TimeoutPattern(false, 0),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
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
                sagaPattern.sagaPivotTransactions()
        );
    }
}