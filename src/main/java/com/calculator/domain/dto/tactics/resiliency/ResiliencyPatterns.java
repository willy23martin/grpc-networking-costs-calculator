package com.calculator.domain.dto.tactics.resiliency;

import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ResiliencyPatterns(
        @JsonProperty("timeoutPattern")
        TimeoutPattern timeoutPattern,
        @JsonProperty("retryPattern")
        RetryPattern retryPattern,
        @JsonProperty("circuitBreakerPattern")
        CircuitBreakerPattern circuitBreakerPattern

) {
    public static ResiliencyPatterns empty() {
        return new ResiliencyPatterns(
                TimeoutPattern.empty(),
                RetryPattern.empty(),
                CircuitBreakerPattern.empty()
        );
    }
}
