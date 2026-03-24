package com.calculator.domain.dto.tactics.resiliency;

public record RetryPattern(
        boolean resiliencyRetryTactic,
        int tacticRetryTimes
) {
    public static RetryPattern empty(){
       return new RetryPattern(false, 0);
    }
}