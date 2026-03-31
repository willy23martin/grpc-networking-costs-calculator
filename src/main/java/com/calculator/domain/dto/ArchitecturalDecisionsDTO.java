package com.calculator.domain.dto;

import com.calculator.domain.dto.tactics.microservices.SAGAPattern;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;

public record ArchitecturalDecisionsDTO(
        long requestsPerSecond,
        ReliabilityTactics reliabilityTactics,
        TimeoutPattern timeoutTactic,
        RetryPattern retryTactic,
        CircuitBreakerPattern circuitBreakerTactic,
        SAGAPattern sagaPattern,
        SecurityTactics securityTactics
) {
    public static ArchitecturalDecisionsDTO empty() {
        return new ArchitecturalDecisionsDTO(
                0,
                ReliabilityTactics.empty(),
                TimeoutPattern.empty(),
                RetryPattern.empty(),
                CircuitBreakerPattern.empty(),
                SAGAPattern.empty(),
                SecurityTactics.empty()
        );
    }
}