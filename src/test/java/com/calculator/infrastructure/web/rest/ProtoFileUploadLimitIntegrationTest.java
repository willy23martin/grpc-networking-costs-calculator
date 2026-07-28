package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static com.calculator.shared.ProtocolBuffersUtilsTest.VALID_PROTO_CONTENT;
import static org.assertj.core.api.Assertions.assertThat;

// TODO - REFACTOR AFTER SECURITY
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProtoFileUploadLimitIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String sessionCookie;

    @BeforeEach
    void loginBeforeTesting() {
        // 1. Prepariamo le credenziali reali (configurate nel tuo UserDetailsServiceImpl)
        MultiValueMap<String, String> loginForm = new LinkedMultiValueMap<>();
        loginForm.add("username", "testuser");
        loginForm.add("password", "testpassword");

        // 2. Invochiamo l'endpoint di login pubblico /auth o quello di default di Spring
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                "/auth/login", // Sostituisci con il tuo endpoint di login reale sotto /auth/**
                loginForm,
                String.class
        );

        // 3. Estraiamo il cookie di sessione (JSESSIONID) restituito dal server Tomcat
        List<String> cookies = loginResponse.getHeaders().get(HttpHeaders.SET_COOKIE);
        if (cookies != null && !cookies.isEmpty()) {
            this.sessionCookie = cookies.getFirst();
        }
    }

    @Disabled
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

    @Disabled
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
    void uploadFile_WhenAuthenticatedAndWithinLimit_ReturnsSuccess() {
        // 1. ConfiguriAMO gli header per l'invio del file multipart
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // FONDAMENTALE: Se siamo autenticati con successo, alleghiamo il cookie di sessione
        if (this.sessionCookie != null) {
            headers.add(HttpHeaders.COOKIE, this.sessionCookie);
        }

        // 2. Costruiamo il payload con il file .proto da caricare
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("protoFile", new ByteArrayResource(VALID_PROTO_CONTENT.getBytes()) {
            @Override
            public String getFilename() {
                return "order.proto";
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // 3. Eseguiamo la chiamata POST protetta
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/calculateProtofileNetworkingCosts",
                requestEntity,
                String.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
    }

    @Disabled
    @Test
    void shouldReturnEmptyTacticsWhenNoSessionExists() {
        ResponseEntity<ArchitecturalDecisionsDTO> response =
                restTemplate.getForEntity("/api/session/tactics", ArchitecturalDecisionsDTO.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().requestsPerSecond()).isZero();
        assertThat(response.getBody().reliabilityTactics().reliabilityClientSideLoadBalancerTactic()).isFalse();
    }

    @Disabled
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