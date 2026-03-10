package com.calculator.domain.model.tactics.resiliency;

public record RetryTactic(
        boolean resiliencyRetryTactic,
        int  tacticRetryTimes
) {
}
