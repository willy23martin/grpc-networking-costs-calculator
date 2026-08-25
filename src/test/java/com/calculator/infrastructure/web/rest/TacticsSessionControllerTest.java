package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.resiliency.ResiliencyPatterns;
import com.calculator.domain.model.architecture.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static com.calculator.infrastructure.web.rest.helper.TacticsSessionControllerTestHelper.setTacticsSessionJsonFrom;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TacticsSessionControllerTest extends BaseIntegrationTest {

    @Autowired
    TacticsSessionControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldDeserialiseNestedTacticsRecordsFromJson() throws Exception {
        final String body = """
            {
              "requestsPerSecond": 1000,
              "reliabilityTactics": {
                "reliabilityClientSideLoadBalancerTactic": true,
                "reliabilityServerSideLoadBalancerTactic": false
              },
              "resiliencyPatterns": {
                "timeoutPattern": {
                  "resiliencyTimeoutPattern": true,
                  "patternTimeoutMilliseconds": 300
                },
                "retryPattern": {
                  "resiliencyRetryPattern": true,
                  "patternRetryTimes": 3
                },
                "circuitBreakerPattern": {
                  "resiliencyCircuitBreakerPattern": true,
                  "circuitBreakerPatternMinimumCalls": 10,
                  "circuitBreakerHalfOpen": 5,
                  "circuitBreakerWaitMilliseconds": 60000,
                  "circuitBreakerFailureRate": 50
                }
              },
              "securityTactics": {
                "tlsTactic": {
                  "tlsEnabled": true,
                  "mtlsEnabled": false,
                  "tlsReconnectsPerHour": 2
                },
                "jwtTactic": {
                  "oauthJwtEnabled": true,
                  "tokenValidationMode": "LOCAL",
                  "tokenTtlSeconds": 3600,
                  "concurrentClients": 4,
                  "interceptorType": "UNARY"
                },
                "basicAuthenticationPattern": {
                  "basicAuthEnabled": false
                }
              }
            }
            """;

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/session/tactics")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reliabilityTactics").isNotEmpty())
                .andExpect(jsonPath("$.resiliencyPatterns.timeoutPattern").isNotEmpty())
                .andExpect(jsonPath("$.resiliencyPatterns.retryPattern").isNotEmpty())
                .andExpect(jsonPath("$.resiliencyPatterns.circuitBreakerPattern").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.tlsTactic").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.jwtTactic").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern").isNotEmpty())
                .andExpect(jsonPath("$.requestsPerSecond").value(1000))
                .andExpect(jsonPath("$.reliabilityTactics.reliabilityClientSideLoadBalancerTactic").value(true))
                .andExpect(jsonPath("$.reliabilityTactics.reliabilityServerSideLoadBalancerTactic").value(false))
                .andExpect(jsonPath("$.resiliencyPatterns.timeoutPattern.resiliencyTimeoutPattern").value(true))
                .andExpect(jsonPath("$.resiliencyPatterns.timeoutPattern.patternTimeoutMilliseconds").value(300))
                .andExpect(jsonPath("$.resiliencyPatterns.retryPattern.resiliencyRetryPattern").value(true))
                .andExpect(jsonPath("$.resiliencyPatterns.retryPattern.patternRetryTimes").value(3))
                .andExpect(jsonPath("$.resiliencyPatterns.circuitBreakerPattern.resiliencyCircuitBreakerPattern").value(true))
                .andExpect(jsonPath("$.resiliencyPatterns.circuitBreakerPattern.circuitBreakerPatternMinimumCalls").value(10))
                .andExpect(jsonPath("$.resiliencyPatterns.circuitBreakerPattern.circuitBreakerHalfOpen").value(5))
                .andExpect(jsonPath("$.resiliencyPatterns.circuitBreakerPattern.circuitBreakerWaitMilliseconds").value(60000))
                .andExpect(jsonPath("$.resiliencyPatterns.circuitBreakerPattern.circuitBreakerFailureRate").value(50))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.mtlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsReconnectsPerHour").value(2))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.tokenValidationMode").value(OAuthTokenValidationModes.LOCAL.toString()))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.tokenTtlSeconds").value(3600))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.concurrentClients").value(4))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.interceptorType").value(InterceptorType.UNARY.toString()))
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(false));
    }

    @Test
    void shouldSaveTacticsToSessionAndReturnNoContentWhenNoTacticsAreSelected() throws Exception {
        final ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true, false),
                new ResiliencyPatterns(
                        new TimeoutPattern(true, 300),
                        new RetryPattern(true, 3),
                        new CircuitBreakerPattern(false, 0, 0, 0, 0)
                ),
                SecurityTactics.empty()
        );
        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setTacticsSessionJsonFrom(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturnEmptyDtoWhenNoSessionExists() throws Exception {
        mockMvc.perform(get("/api/session/tactics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestsPerSecond").value(0))
                .andExpect(jsonPath("$.reliabilityTactics.reliabilityClientSideLoadBalancerTactic").value(false))
                .andExpect(jsonPath("$.reliabilityTactics.reliabilityServerSideLoadBalancerTactic").value(false))
                .andExpect(jsonPath("$.resiliencyPatterns.timeoutPattern.resiliencyTimeoutPattern").value(false))
                .andExpect(jsonPath("$.resiliencyPatterns.retryPattern.resiliencyRetryPattern").value(false))
                .andExpect(jsonPath("$.resiliencyPatterns.circuitBreakerPattern.resiliencyCircuitBreakerPattern").value(false))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.mtlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(false));
    }

    @Test
    void shouldReturnStoredTacticsFromSession() throws Exception {
        final ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                500,
                new ReliabilityTactics(true, false),
                new ResiliencyPatterns(
                        new TimeoutPattern(true, 200),
                        new RetryPattern(false, 0),
                        new CircuitBreakerPattern(false, 0, 0, 0, 0)
                ),
                SecurityTactics.empty()
        );
        mockMvc.perform(get("/api/session/tactics")
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestsPerSecond").value(500))
                .andExpect(jsonPath("$.reliabilityTactics.reliabilityClientSideLoadBalancerTactic").value(true))
                .andExpect(jsonPath("$.resiliencyPatterns.timeoutPattern.patternTimeoutMilliseconds").value(200))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(false));
    }

    @Test
    void shouldClearTacticsFromSessionAndReturnNoContent() throws Exception {
        final ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true, true),
                new ResiliencyPatterns(
                        new TimeoutPattern(false, 0),
                        new RetryPattern(false, 0),
                        new CircuitBreakerPattern(false, 0, 0, 0, 0)
                ),
                SecurityTactics.empty()
        );
        mockMvc.perform(delete("/api/session/tactics")
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isNoContent());
    }

}