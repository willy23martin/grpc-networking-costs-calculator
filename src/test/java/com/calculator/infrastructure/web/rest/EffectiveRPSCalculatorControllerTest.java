package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.rps.RequestPerSecondCostCalculatorService;
import com.calculator.domain.dto.requests.EffectiveRequestPerSecondRequest;
import com.calculator.domain.dto.responses.EffectiveRequestPerSecondResponse;
import com.calculator.domain.model.architecture.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EffectiveRPSCalculatorControllerTest {

    @Mock
    private RequestPerSecondCostCalculatorService requestPerSecondCostCalculatorService;

    @InjectMocks
    private EffectiveRPSCalculatorController controller;

    @Test
    void calculateEffectiveRps_zeroOrNegativeBaseRps() {
        final EffectiveRequestPerSecondRequest req = new EffectiveRequestPerSecondRequest();
        req.setBaseRequestPerSecond(0);

        final ResponseEntity<EffectiveRequestPerSecondResponse> response = controller.calculateEffectiveRps(req);
        EffectiveRequestPerSecondResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(0, resp.getEffectiveRps());
        assertFalse(resp.isRpsWasAdjusted());
    }

    @Test
    void calculateEffectiveRps_noOverheadFallback() {
        final EffectiveRequestPerSecondRequest req = new EffectiveRequestPerSecondRequest();
        req.setBaseRequestPerSecond(100);
        req.setInterceptorType(InterceptorType.UNARY.name());
        req.setTokenValidationMode(OAuthTokenValidationModes.LOCAL.name());

        when(requestPerSecondCostCalculatorService.calculateEffectiveRequestsPerSecond(any())).thenReturn(100L);

        final ResponseEntity<EffectiveRequestPerSecondResponse> response = controller.calculateEffectiveRps(req);
        final EffectiveRequestPerSecondResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(100, resp.getEffectiveRps());
        assertFalse(resp.isRpsWasAdjusted());
        assertEquals("100 base = 100 req/s", resp.getOauthPreview());
    }

    @Test
    void calculateEffectiveRps_tlsAndOauthRemoteOverhead() {
        final EffectiveRequestPerSecondRequest req = new EffectiveRequestPerSecondRequest();
        req.setBaseRequestPerSecond(1000);
        req.setInterceptorType(InterceptorType.STREAM.name());
        req.setTokenValidationMode(OAuthTokenValidationModes.REMOTE_INTROSPECTION.name());
        req.setTlsEnabled(true);
        req.setMtlsEnabled(false);
        req.setTlsReconnectsPerHour(3600);
        req.setOauthEnabled(true);
        req.setTokenTtlSeconds(60);
        req.setConcurrentClients(2);
        req.setRetryEnabled(true);
        req.setRetryErrorPercentage(10);

        when(requestPerSecondCostCalculatorService.calculateEffectiveRequestsPerSecond(any())).thenReturn(2100L);

        final ResponseEntity<EffectiveRequestPerSecondResponse> response = controller.calculateEffectiveRps(req);
        final EffectiveRequestPerSecondResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(2100, resp.getEffectiveRps());
        assertTrue(resp.isRpsWasAdjusted());
        assertEquals(2, resp.getHandshakeExtra());
        assertEquals(8, resp.getTokenAcqExtra());
        assertEquals(1000, resp.getIntrospectionExtra());
        assertEquals(201, resp.getRetryExtra());
        assertFalse(resp.getBreakdown().isEmpty());
        assertNotNull(resp.getOauthPreview());
    }

    @Test
    void calculateEffectiveRps_mtlsAndOauthLocalOverheadWithDefaults() {
        final EffectiveRequestPerSecondRequest req = new EffectiveRequestPerSecondRequest();
        req.setBaseRequestPerSecond(500);
        req.setInterceptorType(InterceptorType.UNARY.name());
        req.setTokenValidationMode(OAuthTokenValidationModes.LOCAL.name());
        req.setTlsEnabled(false);
        req.setMtlsEnabled(true);
        req.setTlsReconnectsPerHour(7200);
        req.setOauthEnabled(true);
        req.setTokenTtlSeconds(0);
        req.setConcurrentClients(0);
        req.setRetryEnabled(false);

        when(requestPerSecondCostCalculatorService.calculateEffectiveRequestsPerSecond(any())).thenReturn(600L);

        final ResponseEntity<EffectiveRequestPerSecondResponse> response = controller.calculateEffectiveRps(req);
        final EffectiveRequestPerSecondResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(600, resp.getEffectiveRps());
        assertTrue(resp.isRpsWasAdjusted());
        assertEquals(10, resp.getHandshakeExtra());
        assertEquals(0, resp.getIntrospectionExtra());
        assertNotNull(resp.getOauthPreview());
    }
}