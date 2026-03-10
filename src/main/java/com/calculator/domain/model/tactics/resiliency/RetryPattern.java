package com.calculator.domain.model.tactics.resiliency;

public record RetryPattern(
        boolean resiliencyRetryTactic,
        int tacticRetryTimes
) { }