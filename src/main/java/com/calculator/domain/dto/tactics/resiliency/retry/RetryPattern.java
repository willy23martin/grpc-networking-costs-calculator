package com.calculator.domain.dto.tactics.resiliency.retry;

public record RetryPattern(
        boolean resiliencyRetryTactic,
        int tacticRetryTimes
) {
    public static RetryPattern empty(){
       return new RetryPattern(false, 0);
    }
}