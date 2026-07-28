package com.calculator.domain.dto.tactics.resiliency;

public record TimeoutPattern(
        boolean resiliencyTimeoutPattern,
        long patternTimeoutMilliseconds
) {
    public static TimeoutPattern empty(){
        return new TimeoutPattern(false, 0);
    }
}