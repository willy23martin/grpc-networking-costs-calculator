package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.calculator.shared.ProtocolBuffersUtilsTest.VALID_PROTO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProtoFileUploadLimitIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSaveTacticsToSessionAndReturnNoContent() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true,  false),
                new TimeoutPattern(true,  300),
                new RetryPattern(true,  3),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                SecurityTactics.empty()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ArchitecturalDecisionsDTO> request = new HttpEntity<>(dto, headers);
        ResponseEntity<Void> response =
                restTemplate.postForEntity("/api/session/tactics", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldNotThrowFileCountLimitExceededExceptionWhenUploadingProtoFileOnly() {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true, false),
                new TimeoutPattern(true, 300),
                new RetryPattern(true, 3),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                SecurityTactics.empty()
        );
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/session/tactics",
                new HttpEntity<>(dto, jsonHeaders), Void.class);

        HttpHeaders multipartHeaders = new HttpHeaders();
        multipartHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("protoFile", new ByteArrayResource(VALID_PROTO_CONTENT.getBytes()) {
            @Override public String getFilename() { return "order.proto"; }
        });

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/calculateProtofileNetworkingCosts",
                new HttpEntity<>(body, multipartHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnEmptyTacticsWhenNoSessionExists() {
        ResponseEntity<ArchitecturalDecisionsDTO> response =
                restTemplate.getForEntity("/api/session/tactics", ArchitecturalDecisionsDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requestsPerSecond()).isZero();
        assertThat(response.getBody().reliabilityTactics().reliabilityClientSideLoadBalancerTactic()).isFalse();
    }

    @Test
    void shouldClearTacticsFromSession() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                500,
                new ReliabilityTactics(true, true),
                new TimeoutPattern(false, 0),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                SecurityTactics.empty()
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/session/tactics", new HttpEntity<>(dto, headers), Void.class);

        restTemplate.delete("/api/session/tactics");

        ResponseEntity<ArchitecturalDecisionsDTO> response =
                restTemplate.getForEntity("/api/session/tactics", ArchitecturalDecisionsDTO.class);
        assertThat(response.getBody().requestsPerSecond()).isZero();
        assertThat(response.getBody().reliabilityTactics().reliabilityClientSideLoadBalancerTactic()).isFalse();
    }
}