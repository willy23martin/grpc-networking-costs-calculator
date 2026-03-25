package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.dto.tactics.microservices.SAGAPattern;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.dto.tactics.security.oauth.OAuthTokenValidationMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static com.calculator.infrastructure.web.rest.helper.TacticsSessionControllerTestHelper.setTacticsSessionJsonFrom;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TacticsSessionControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    TacticsSessionControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldDeserialiseNestedTacticsRecordsFromJson() throws Exception {
        String body = """
            {
              "requestsPerSecond": 1000,
              "reliabilityTactics": {
                "reliabilityClientSideLoadBalancerTactic": true,
                "reliabilityServerSideLoadBalancerTactic": false
              },
              "timeoutTactic": {
                "resiliencyTimeoutTactic": true,
                "tacticTimeoutMilliseconds": 300
              },
              "retryTactic": {
                "resiliencyRetryTactic": true,
                "tacticRetryTimes": 3
              },
              "circuitBreakerTactic": {
                "resiliencyCircuitBreakerPattern": true,
                "circuitBreakerPatternMinimumCalls": 10,
                "circuitBreakerHalfOpen": 5,
                "circuitBreakerWaitMilliseconds": 60000,
                "circuitBreakerFailureRate": 50
              },
              "sagaPattern": {
                "microservicesSAGAPattern": true,
                "sagaCompensatableTransactions": 1,
                "sagaRetriableTransactions": 2,
                "sagaPivotTransactions": 1
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
                .andExpect(jsonPath("$.timeoutTactic").isNotEmpty())
                .andExpect(jsonPath("$.retryTactic").isNotEmpty())
                .andExpect(jsonPath("$.circuitBreakerTactic").isNotEmpty())
                .andExpect(jsonPath("$.sagaPattern").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.tlsTactic").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.jwtTactic").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern").isNotEmpty())
                .andExpect(jsonPath("$.requestsPerSecond").value(1000))
                .andExpect(jsonPath("$.reliabilityTactics.reliabilityClientSideLoadBalancerTactic").value(true))
                .andExpect(jsonPath("$.reliabilityTactics.reliabilityServerSideLoadBalancerTactic").value(false))
                .andExpect(jsonPath("$.timeoutTactic.resiliencyTimeoutTactic").value(true))
                .andExpect(jsonPath("$.timeoutTactic.tacticTimeoutMilliseconds").value(300))
                .andExpect(jsonPath("$.retryTactic.resiliencyRetryTactic").value(true))
                .andExpect(jsonPath("$.retryTactic.tacticRetryTimes").value(3))
                .andExpect(jsonPath("$.circuitBreakerTactic.resiliencyCircuitBreakerPattern").value(true))
                .andExpect(jsonPath("$.circuitBreakerTactic.circuitBreakerPatternMinimumCalls").value(10))
                .andExpect(jsonPath("$.circuitBreakerTactic.circuitBreakerHalfOpen").value(5))
                .andExpect(jsonPath("$.circuitBreakerTactic.circuitBreakerWaitMilliseconds").value(60000))
                .andExpect(jsonPath("$.circuitBreakerTactic.circuitBreakerFailureRate").value(50))
                .andExpect(jsonPath("$.sagaPattern.microservicesSAGAPattern").value(true))
                .andExpect(jsonPath("$.sagaPattern.sagaCompensatableTransactions").value(1))
                .andExpect(jsonPath("$.sagaPattern.sagaRetriableTransactions").value(2))
                .andExpect(jsonPath("$.sagaPattern.sagaPivotTransactions").value(1))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.mtlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsReconnectsPerHour").value(2))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.tokenValidationMode").value(OAuthTokenValidationMode.LOCAL.toString()))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.tokenTtlSeconds").value(3600))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.concurrentClients").value(4))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.interceptorType").value(InterceptorType.UNARY.toString()))
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(false));
    }

    @Test
    void shouldSaveTacticsToSessionAndReturnNoContentWhenNoTacticsAreSelected() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true, false),
                new TimeoutPattern(true, 300),
                new RetryPattern(true, 3),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0),
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
                .andExpect(jsonPath("$.timeoutTactic.resiliencyTimeoutTactic").value(false))
                .andExpect(jsonPath("$.retryTactic.resiliencyRetryTactic").value(false))
                .andExpect(jsonPath("$.circuitBreakerTactic.resiliencyCircuitBreakerPattern").value(false))
                .andExpect(jsonPath("$.sagaPattern.microservicesSAGAPattern").value(false))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.mtlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(false));
    }

    @Test
    void shouldReturnStoredTacticsFromSession() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                500,
                new ReliabilityTactics(true, false),
                new TimeoutPattern(true, 200),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(true, 1, 2, 1),
                SecurityTactics.empty()
        );
        mockMvc.perform(get("/api/session/tactics")
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestsPerSecond").value(500))
                .andExpect(jsonPath("$.reliabilityTactics.reliabilityClientSideLoadBalancerTactic").value(true))
                .andExpect(jsonPath("$.timeoutTactic.tacticTimeoutMilliseconds").value(200))
                .andExpect(jsonPath("$.sagaPattern.microservicesSAGAPattern").value(true))
                .andExpect(jsonPath("$.sagaPattern.sagaCompensatableTransactions").value(1))
                .andExpect(jsonPath("$.sagaPattern.sagaRetriableTransactions").value(2))
                .andExpect(jsonPath("$.sagaPattern.sagaPivotTransactions").value(1))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(false));
    }

    @Test
    void shouldClearTacticsFromSessionAndReturnNoContent() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true, true),
                new TimeoutPattern(false, 0),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0),
                SecurityTactics.empty()
        );
        mockMvc.perform(delete("/api/session/tactics")
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isNoContent());
    }

}