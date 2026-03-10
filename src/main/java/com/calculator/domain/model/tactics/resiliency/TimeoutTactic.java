package com.calculator.domain.model.tactics.resiliency;

public record TimeoutTactic(
        boolean resiliencyTimeoutTactic,
        int tacticTimeoutMilliseconds
) {
}
