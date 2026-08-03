package com.calculator.infrastructure.web.rest.helper;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.ResiliencyPatterns;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;

public class TacticsSessionControllerTestHelper {

    public static ArchitecturalDecisionsDTO setTacticsDto(long rps, SecurityTactics securityTactics) {
        return new ArchitecturalDecisionsDTO(
                rps,
                new ReliabilityTactics(false, false),
                new ResiliencyPatterns(
                        new TimeoutPattern(false, 0),
                        new RetryPattern(false, 0),
                        new CircuitBreakerPattern(false, 0, 0, 0, 0)
                ),
                securityTactics
        );
    }

    public static String setTacticsSessionJsonFrom(ArchitecturalDecisionsDTO architecturalDecisionsDTO) {
        SecurityTactics securityTactics = architecturalDecisionsDTO.securityTactics();
        return """
                {
                  "requestsPerSecond": %d,
                  "reliabilityTactics": {
                    "reliabilityClientSideLoadBalancerTactic": %b,
                    "reliabilityServerSideLoadBalancerTactic": %b
                  },
                  "timeoutTactic": {
                    "resiliencyTimeoutTactic": %b,
                    "tacticTimeoutMilliseconds": %d
                  },
                  "retryTactic": {
                    "resiliencyRetryTactic": %b,
                    "tacticRetryTimes": %d
                  },
                  "circuitBreakerPattern": {
                    "resiliencyCircuitBreakerPattern": %b,
                    "circuitBreakerPatternMinimumCalls": %d,
                    "circuitBreakerHalfOpen": %d,
                    "circuitBreakerWaitMilliseconds": %d,
                    "circuitBreakerFailureRate": %d
                  },
                  "securityTactics": {
                    "tlsTactic": {
                      "tlsEnabled": %b,
                      "mtlsEnabled": %b,
                      "tlsReconnectsPerHour": %d
                    },
                    "jwtTactic": {
                      "oauthJwtEnabled": %b,
                      "tokenValidationMode": "%s",
                      "tokenTtlSeconds": %d,
                      "concurrentClients": %d,
                      "interceptorType": "%s"
                    },
                    "basicAuthenticationPattern": {
                      "basicAuthEnabled": %b
                    }
                  }
                }
                """.formatted(
                architecturalDecisionsDTO.requestsPerSecond(),
                architecturalDecisionsDTO.reliabilityTactics().reliabilityClientSideLoadBalancerTactic(),
                architecturalDecisionsDTO.reliabilityTactics().reliabilityServerSideLoadBalancerTactic(),
                architecturalDecisionsDTO.resiliencyPatterns().timeoutPattern().resiliencyTimeoutPattern(),
                architecturalDecisionsDTO.resiliencyPatterns().timeoutPattern().patternTimeoutMilliseconds(),
                architecturalDecisionsDTO.resiliencyPatterns().retryPattern().resiliencyRetryPattern(),
                architecturalDecisionsDTO.resiliencyPatterns().retryPattern().patternRetryTimes(),
                architecturalDecisionsDTO.resiliencyPatterns().circuitBreakerPattern().resiliencyCircuitBreakerPattern(),
                architecturalDecisionsDTO.resiliencyPatterns().circuitBreakerPattern().circuitBreakerPatternMinimumCalls(),
                architecturalDecisionsDTO.resiliencyPatterns().circuitBreakerPattern().circuitBreakerHalfOpen(),
                architecturalDecisionsDTO.resiliencyPatterns().circuitBreakerPattern().circuitBreakerWaitMilliseconds(),
                architecturalDecisionsDTO.resiliencyPatterns().circuitBreakerPattern().circuitBreakerFailureRate(),
                securityTactics.tlsTactic().tlsEnabled(),
                securityTactics.tlsTactic().mtlsEnabled(),
                securityTactics.tlsTactic().tlsReconnectsPerHour(),
                securityTactics.jwtTactic().oauthJwtEnabled(),
                securityTactics.jwtTactic().tokenValidationMode(),
                securityTactics.jwtTactic().tokenTtlSeconds(),
                securityTactics.jwtTactic().concurrentClients(),
                securityTactics.jwtTactic().interceptorType(),
                securityTactics.basicAuthenticationPattern().basicAuthEnabled()
        );
    }

}
