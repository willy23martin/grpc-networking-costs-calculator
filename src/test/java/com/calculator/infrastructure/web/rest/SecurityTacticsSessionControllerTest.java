package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.model.architecture.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.dto.tactics.security.authentication.BasicAuthenticationPattern;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import com.calculator.domain.dto.tactics.security.oauth.jwt.JWTTactic;
import com.calculator.domain.dto.tactics.security.tls.TLSTactic;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static com.calculator.infrastructure.web.rest.helper.TacticsSessionControllerTestHelper.setTacticsDto;
import static com.calculator.infrastructure.web.rest.helper.TacticsSessionControllerTestHelper.setTacticsSessionJsonFrom;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityTacticsSessionControllerTest extends BaseIntegrationTest{

    @Autowired
    SecurityTacticsSessionControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void shouldSaveAndReturnTlsSecurityTactics() throws Exception {
        SecurityTactics securityTactics = new SecurityTactics(
                new TLSTactic(true, false, 4),
                new JWTTactic(false, OAuthTokenValidationModes.LOCAL, 3600, 1, InterceptorType.UNARY),
                new BasicAuthenticationPattern(false)
        );
        ArchitecturalDecisionsDTO architecturalDecisionsDTO = setTacticsDto(1000, securityTactics);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setTacticsSessionJsonFrom(architecturalDecisionsDTO))
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/session/tactics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityTactics").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.mtlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsReconnectsPerHour").value(4))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(false));
    }

    @Test
    void shouldSaveAndReturnMtlsSecurityTactics() throws Exception {
        SecurityTactics mtls = new SecurityTactics(
                new TLSTactic(true, true, 6),
                new JWTTactic(false, OAuthTokenValidationModes.LOCAL, 3600, 1, InterceptorType.UNARY),
                new BasicAuthenticationPattern(false)
        );
        ArchitecturalDecisionsDTO dto = setTacticsDto(1000, mtls);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setTacticsSessionJsonFrom(dto))
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/session/tactics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityTactics").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.mtlsEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsReconnectsPerHour").value(6))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(false));
    }

    @Test
    void shouldSaveAndReturnOAuthJwtLocalValidation() throws Exception {
        SecurityTactics oauth = new SecurityTactics(
                new TLSTactic(false, false, 0),
                new JWTTactic(true, OAuthTokenValidationModes.LOCAL, 1800, 3, InterceptorType.UNARY),
                new BasicAuthenticationPattern(false)
        );
        ArchitecturalDecisionsDTO dto = setTacticsDto(2000, oauth);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setTacticsSessionJsonFrom(dto))
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/session/tactics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityTactics").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.tokenValidationMode").value(OAuthTokenValidationModes.LOCAL.toString()))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.tokenTtlSeconds").value(1800))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.concurrentClients").value(3))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.interceptorType").value(InterceptorType.UNARY.toString()))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(false));
    }

    @Test
    void shouldSaveAndReturnOAuthJwtRemoteIntrospection() throws Exception {
        SecurityTactics oauth = new SecurityTactics(
                new TLSTactic(false, false, 0),
                new JWTTactic(true, OAuthTokenValidationModes.REMOTE_INTROSPECTION, 3600, 1, InterceptorType.STREAM),
                new BasicAuthenticationPattern(false)
        );
        ArchitecturalDecisionsDTO dto = setTacticsDto(500, oauth);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setTacticsSessionJsonFrom(dto))
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/session/tactics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityTactics").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.tokenValidationMode").value(OAuthTokenValidationModes.REMOTE_INTROSPECTION.toString()))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.interceptorType").value(InterceptorType.STREAM.toString()))
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(false));
    }

    @Test
    void shouldSaveAndReturnBasicAuthSecurityTactic() throws Exception {
        SecurityTactics basicAuth = new SecurityTactics(
                new TLSTactic(false, false, 0),
                new JWTTactic(false, OAuthTokenValidationModes.LOCAL, 3600, 1, InterceptorType.UNARY),
                new BasicAuthenticationPattern(true)
        );
        ArchitecturalDecisionsDTO dto = setTacticsDto(300, basicAuth);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setTacticsSessionJsonFrom(dto))
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/session/tactics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityTactics").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(false))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(false));
    }

    @Test
    void shouldSaveAndReturnAllSecurityTacticsEnabled() throws Exception {
        SecurityTactics allSecurityTactics = new SecurityTactics(
                new TLSTactic(true, true, 10),
                new JWTTactic(true, OAuthTokenValidationModes.REMOTE_INTROSPECTION, 900, 5, InterceptorType.STREAM),
                new BasicAuthenticationPattern(false)
        );
        ArchitecturalDecisionsDTO dto = setTacticsDto(5000, allSecurityTactics);
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(setTacticsSessionJsonFrom(dto))
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/session/tactics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.securityTactics").isNotEmpty())
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.mtlsEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.tlsTactic.tlsReconnectsPerHour").value(10))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.oauthJwtEnabled").value(true))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.tokenValidationMode").value(OAuthTokenValidationModes.REMOTE_INTROSPECTION.toString()))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.tokenTtlSeconds").value(900))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.concurrentClients").value(5))
                .andExpect(jsonPath("$.securityTactics.jwtTactic.interceptorType").value(InterceptorType.STREAM.toString()))
                .andExpect(jsonPath("$.securityTactics.basicAuthenticationPattern.basicAuthEnabled").value(false));
    }

    @Test
    void shouldHandleMissingSecurityTacticsBlockGracefully() throws Exception {
        String bodyWithoutSecurity = """
            {
              "requestsPerSecond": 1000,
              "reliabilityTactics": {
                "reliabilityClientSideLoadBalancerTactic": false,
                "reliabilityServerSideLoadBalancerTactic": false
              },
              "timeoutTactic": { "resiliencyTimeoutTactic": false, "tacticTimeoutMilliseconds": 0 },
              "retryTactic":   { "resiliencyRetryTactic": false,   "tacticRetryTimes": 0 },
              "circuitBreakerTactic": {
                "resiliencyCircuitBreakerPattern": false,
                "circuitBreakerPatternMinimumCalls": 0,
                "circuitBreakerHalfOpen": 0,
                "circuitBreakerWaitMilliseconds": 0,
                "circuitBreakerFailureRate": 0
              }
            }
            """;

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyWithoutSecurity)
                        .session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/session/tactics").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestsPerSecond").value(1000));
    }
}
