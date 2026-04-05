package com.calculator.domain.dto.tactics.resiliency;

public record TimeoutPattern(
        boolean resiliencyTimeoutTactic,
        long tacticTimeoutMilliseconds
) {
    public static TimeoutPattern empty(){
        return new TimeoutPattern(false, 0);
    }
}