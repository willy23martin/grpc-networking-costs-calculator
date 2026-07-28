package com.calculator.domain.dto.tactics.resiliency.retry;

public record RetryPattern(
        boolean resiliencyRetryPattern,
        int patternRetryTimes
) {
    public static RetryPattern empty(){
       return new RetryPattern(false, 0);
    }
}