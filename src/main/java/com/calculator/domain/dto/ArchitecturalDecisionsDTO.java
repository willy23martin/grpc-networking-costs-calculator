package com.calculator.domain.dto;

import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.ResiliencyPatterns;
import com.calculator.domain.dto.tactics.security.SecurityTactics;

public record ArchitecturalDecisionsDTO(
        long requestsPerSecond,
        ReliabilityTactics reliabilityTactics,
        ResiliencyPatterns resiliencyPatterns,
        SecurityTactics securityTactics
) {
    public static ArchitecturalDecisionsDTO empty() {
        return new ArchitecturalDecisionsDTO(
                0,
                ReliabilityTactics.empty(),
                ResiliencyPatterns.empty(),
                SecurityTactics.empty()
        );
    }
}