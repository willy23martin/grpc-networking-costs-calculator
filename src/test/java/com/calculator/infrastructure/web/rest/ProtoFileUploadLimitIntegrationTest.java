package com.calculator.infrastructure.web.rest;

import com.calculator.domain.model.tactics.TacticsConfigDTO;
import com.calculator.domain.model.tactics.microservices.SAGAPattern;
import com.calculator.domain.model.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.model.tactics.resiliency.CircuitBreakerTactic;
import com.calculator.domain.model.tactics.resiliency.RetryTactic;
import com.calculator.domain.model.tactics.resiliency.TimeoutTactic;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static com.calculator.infrastructure.web.rest.TCOCalculatorControllerTest.VALID_PROTO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProtoFileUploadLimitIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldSaveTacticsToSessionAndReturnNoContent() throws Exception {
        TacticsConfigDTO dto = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(true,  false),
                new TimeoutTactic(true,  300),
                new RetryTactic(true,  3),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<TacticsConfigDTO> request = new HttpEntity<>(dto, headers);
        ResponseEntity<Void> response =
                restTemplate.postForEntity("/api/session/tactics", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldNotThrowFileCountLimitExceededExceptionWhenUploadingProtoFileOnly() {
        TacticsConfigDTO dto = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(true, false),
                new TimeoutTactic(true, 300),
                new RetryTactic(true, 3),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
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
                "/calculateTCO",
                new HttpEntity<>(body, multipartHeaders),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shouldReturnEmptyTacticsWhenNoSessionExists() {
        ResponseEntity<TacticsConfigDTO> response =
                restTemplate.getForEntity("/api/session/tactics", TacticsConfigDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requestsPerSecond()).isZero();
        assertThat(response.getBody().reliabilityTactics().reliabilityClientSideLoadBalancerTactic()).isFalse();
        assertThat(response.getBody().sagaPattern().microservicesSAGAPattern()).isFalse();
    }

    @Test
    void shouldClearTacticsFromSession() throws Exception {
        TacticsConfigDTO dto = new TacticsConfigDTO(
                500,
                new ReliabilityTactics(true, true),
                new TimeoutTactic(false, 0),
                new RetryTactic(false, 0),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
        );
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.postForEntity("/api/session/tactics", new HttpEntity<>(dto, headers), Void.class);

        restTemplate.delete("/api/session/tactics");

        ResponseEntity<TacticsConfigDTO> response =
                restTemplate.getForEntity("/api/session/tactics", TacticsConfigDTO.class);
        assertThat(response.getBody().requestsPerSecond()).isZero();
        assertThat(response.getBody().reliabilityTactics().reliabilityClientSideLoadBalancerTactic()).isFalse();
    }
}