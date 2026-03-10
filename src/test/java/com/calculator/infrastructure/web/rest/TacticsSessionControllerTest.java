package com.calculator.infrastructure.web.rest;

import com.calculator.domain.model.tactics.TacticsConfigDTO;
import com.calculator.domain.model.tactics.microservices.SAGAPattern;
import com.calculator.domain.model.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.model.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.model.tactics.resiliency.RetryPattern;
import com.calculator.domain.model.tactics.resiliency.TimeoutPattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
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
                .andExpect(jsonPath("$.sagaPattern.sagaPivotTransactions").value(1));
    }

    @Test
    void shouldSaveTacticsToSessionAndReturnNoContent() throws Exception {
        TacticsConfigDTO dto = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(true, false),
                new TimeoutPattern(true, 300),
                new RetryPattern(true, 3),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
        );
        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(dto)))
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
                .andExpect(jsonPath("$.sagaPattern.microservicesSAGAPattern").value(false));
    }

    @Test
    void shouldReturnStoredTacticsFromSession() throws Exception {
        TacticsConfigDTO dto = new TacticsConfigDTO(
                500,
                new ReliabilityTactics(true, false),
                new TimeoutPattern(true, 200),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(true, 1, 2, 1)
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
                .andExpect(jsonPath("$.sagaPattern.sagaPivotTransactions").value(1));
    }

    @Test
    void shouldClearTacticsFromSessionAndReturnNoContent() throws Exception {
        TacticsConfigDTO dto = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(true, true),
                new TimeoutPattern(false, 0),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
        );
        mockMvc.perform(delete("/api/session/tactics")
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isNoContent());
    }

    private static String toJson(TacticsConfigDTO dto) {
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
                  "circuitBreakerTactic": {
                    "resiliencyCircuitBreakerPattern": %b,
                    "circuitBreakerPatternMinimumCalls": %d,
                    "circuitBreakerHalfOpen": %d,
                    "circuitBreakerWaitMilliseconds": %d,
                    "circuitBreakerFailureRate": %d
                  },
                  "sagaPattern": {
                    "microservicesSAGAPattern": %b,
                    "sagaCompensatableTransactions": %d,
                    "sagaRetriableTransactions": %d,
                    "sagaPivotTransactions": %d
                  }
                }
                """.formatted(
                dto.requestsPerSecond(),
                dto.reliabilityTactics().reliabilityClientSideLoadBalancerTactic(),
                dto.reliabilityTactics().reliabilityServerSideLoadBalancerTactic(),
                dto.timeoutTactic().resiliencyTimeoutTactic(),
                dto.timeoutTactic().tacticTimeoutMilliseconds(),
                dto.retryTactic().resiliencyRetryTactic(),
                dto.retryTactic().tacticRetryTimes(),
                dto.circuitBreakerTactic().resiliencyCircuitBreakerPattern(),
                dto.circuitBreakerTactic().circuitBreakerPatternMinimumCalls(),
                dto.circuitBreakerTactic().circuitBreakerHalfOpen(),
                dto.circuitBreakerTactic().circuitBreakerWaitMilliseconds(),
                dto.circuitBreakerTactic().circuitBreakerFailureRate(),
                dto.sagaPattern().microservicesSAGAPattern(),
                dto.sagaPattern().sagaCompensatableTransactions(),
                dto.sagaPattern().sagaRetriableTransactions(),
                dto.sagaPattern().sagaPivotTransactions()
        );
    }
}