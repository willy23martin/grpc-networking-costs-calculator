package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.requests.TacticTCCContributionRequest;
import com.calculator.domain.dto.responses.TacticTCCContributionResponse;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TacticsContributionControllerTest extends BaseIntegrationTest{

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calculateTacticContributions_allStructuralAndInfoTactics() throws Exception {
        final TacticTCCContributionRequest req = new TacticTCCContributionRequest();
        req.baseRps = 100;
        req.protoResponseSizeEffectiveBytes = 500;
        req.clientSideLoadBalancingEnabled = true;
        req.serverSideLoadBalancingEnabled = true;
        req.circuitBreakerEnabled = true;
        req.basicAuthEnabled = true;
        req.timeoutEnabled = true;
        req.timeoutMs = 250;

        final MvcResult result = mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        final TacticTCCContributionResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TacticTCCContributionResponse.class
        );

        assertNotNull(resp);
        assertFalse(resp.usedPlaceholderBytes);
        assertEquals(5, resp.contributions.size());
        assertEquals(0.0, resp.totalTacticNetworkingDeltaUsd);
    }

    @Test
    void calculateTacticContributions_retryBranchAndLowCostLabel() throws Exception {
        final TacticTCCContributionRequest req = new TacticTCCContributionRequest();
        req.baseRps = 1;
        req.protoResponseSizeEffectiveBytes = 10;
        req.retryEnabled = true;
        req.retryErrorRatePct = 5.0;

        final MvcResult result = mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        final TacticTCCContributionResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TacticTCCContributionResponse.class
        );

        assertNotNull(resp);
        assertEquals(1, resp.contributions.size());
        assertEquals("< +$0.01/mo", resp.contributions.getFirst().costDisplayLabel);
    }

    @Test
    void calculateTacticContributions_tlsAndMtlsBranches() throws Exception {
        final TacticTCCContributionRequest req1 = new TacticTCCContributionRequest();
        req1.baseRps = 5000;
        req1.protoResponseSizeEffectiveBytes = 0;
        req1.tlsEnabled = true;
        req1.tlsReconnectsPerHour = 3600;
        req1.tlsOverheadBytesFromBackend = 50;

        final MvcResult result1 = mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andReturn();

        final TacticTCCContributionResponse resp1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                TacticTCCContributionResponse.class
        );

        assertNotNull(resp1);
        assertTrue(resp1.usedPlaceholderBytes);
        assertEquals(1, resp1.contributions.size());

        final TacticTCCContributionRequest req2 = new TacticTCCContributionRequest();
        req2.baseRps = 1000;
        req2.mtlsEnabled = true;
        req2.tlsReconnectsPerHour = 0;
        req2.tlsOverheadBytesFromBackend = 0;

        final MvcResult result2 = mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andReturn();

        final TacticTCCContributionResponse resp2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                TacticTCCContributionResponse.class
        );

        assertNotNull(resp2);
        assertEquals("bytes", resp2.contributions.getFirst().kind);
    }

    @Test
    void calculateTacticContributions_oauthLocalAndRemoteModes() throws Exception {
        final TacticTCCContributionRequest req1 = new TacticTCCContributionRequest();
        req1.baseRps = 1000;
        req1.protoResponseSizeEffectiveBytes = 1000;
        req1.oauthEnabled = true;
        req1.tokenValidationMode = OAuthTokenValidationModes.LOCAL.name();
        req1.tokenTtlSeconds = 0;
        req1.concurrentClients = 0;
        req1.jwtOverheadBytesFromBackend = 0;

        final MvcResult result1 = mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andReturn();

        final TacticTCCContributionResponse resp1 = objectMapper.readValue(
                result1.getResponse().getContentAsString(),
                TacticTCCContributionResponse.class
        );

        assertNotNull(resp1);
        assertEquals(0.0, resp1.contributions.getFirst().estimatedMonthlyCostUsd);

        final TacticTCCContributionRequest req2 = new TacticTCCContributionRequest();
        req2.baseRps = 200000;
        req2.protoResponseSizeEffectiveBytes = 2000;
        req2.oauthEnabled = true;
        req2.tokenValidationMode = OAuthTokenValidationModes.REMOTE_INTROSPECTION.name();
        req2.tokenTtlSeconds = 60;
        req2.concurrentClients = 2;
        req2.jwtOverheadBytesFromBackend = 800;

        final MvcResult result2 = mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andReturn();

        final TacticTCCContributionResponse resp2 = objectMapper.readValue(
                result2.getResponse().getContentAsString(),
                TacticTCCContributionResponse.class
        );

        assertNotNull(resp2);
        assertTrue(resp2.totalTacticNetworkingDeltaUsd > 0);
    }

    @Test
    void calculateTacticContributions_allEgressPricingTiers() throws Exception {
        final TacticTCCContributionRequest req = new TacticTCCContributionRequest();
        req.baseRps = 5_000_000;
        req.protoResponseSizeEffectiveBytes = 5_000;
        req.retryEnabled = true;
        req.retryErrorRatePct = 50.0;

        final MvcResult result = mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        final TacticTCCContributionResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TacticTCCContributionResponse.class
        );

        assertNotNull(resp);
        assertTrue(resp.totalTacticNetworkingDeltaUsd > 0);
    }

    @Test
    void calculateTacticContributions_zeroEgressCost() throws Exception {
        final TacticTCCContributionRequest req = new TacticTCCContributionRequest();
        req.baseRps = 0;
        req.protoResponseSizeEffectiveBytes = -100;
        req.retryEnabled = true;

        final MvcResult result = mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        final TacticTCCContributionResponse resp = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                TacticTCCContributionResponse.class
        );

        assertNotNull(resp);
        assertEquals(0.0, resp.totalTacticNetworkingDeltaUsd);
    }
}