package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.ResiliencyPatterns;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static com.calculator.shared.ProtocolBuffersUtilsTest.VALID_PROTO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProtoFileUploadLimitIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSaveTacticsToSessionAndReturnNoContent() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true,  false),
                new ResiliencyPatterns(
                        new TimeoutPattern(true,  300),
                        new RetryPattern(true,  3),
                        new CircuitBreakerPattern(false, 0, 0, 0, 0)
                ),
                SecurityTactics.empty()
        );

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldNotThrowFileCountLimitExceededExceptionWhenUploadingProtoFileOnly() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
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
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        MockMultipartFile protoFile = new MockMultipartFile(
                "protoFile",
                "order.proto",
                MediaType.TEXT_PLAIN_VALUE,
                VALID_PROTO_CONTENT.getBytes()
        );

        MvcResult result = mockMvc.perform(multipart("/calculateProtofileNetworkingCosts")
                        .file(protoFile))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void shouldReturnEmptyTacticsWhenNoSessionExists() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/session/tactics"))
                .andExpect(status().isOk())
                .andReturn();

        ArchitecturalDecisionsDTO responseBody = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ArchitecturalDecisionsDTO.class
        );

        assertThat(responseBody).isNotNull();
        assertThat(responseBody.requestsPerSecond()).isZero();
        assertThat(responseBody.reliabilityTactics().reliabilityClientSideLoadBalancerTactic()).isFalse();
    }

    @Test
    void shouldClearTacticsFromSession() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                500,
                new ReliabilityTactics(true, true),
                new ResiliencyPatterns(
                        new TimeoutPattern(false, 0),
                        new RetryPattern(false, 0),
                        new CircuitBreakerPattern(false, 0, 0, 0, 0)
                ),
                SecurityTactics.empty()
        );

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/session/tactics"))
                .andExpect(status().is2xxSuccessful());

        MvcResult result = mockMvc.perform(get("/api/session/tactics"))
                .andExpect(status().isOk())
                .andReturn();

        ArchitecturalDecisionsDTO responseBody = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                ArchitecturalDecisionsDTO.class
        );

        assertThat(responseBody).isNotNull();
        assertThat(responseBody.requestsPerSecond()).isZero();
        assertThat(responseBody.reliabilityTactics().reliabilityClientSideLoadBalancerTactic()).isFalse();
    }
}