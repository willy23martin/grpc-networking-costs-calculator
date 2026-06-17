package com.calculator.infrastructure.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class TacticsContributionControllerTest {

    private final TacticsContributionController controller = new TacticsContributionController();

    @Test
    void calculateTacticContributions_allStructuralAndInfoTactics() {
        TacticsContributionController.TacticContributionRequest req = new TacticsContributionController.TacticContributionRequest();
        req.baseRps = 100;
        req.protoResponseSizeEffectiveBytes = 500;
        req.clientSideLoadBalancingEnabled = true;
        req.serverSideLoadBalancingEnabled = true;
        req.circuitBreakerEnabled = true;
        req.basicAuthEnabled = true;
        req.timeoutEnabled = true;
        req.timeoutMs = 250;

        ResponseEntity<TacticsContributionController.TacticContributionResponse> response = controller.calculateTacticContributions(req);
        TacticsContributionController.TacticContributionResponse resp = response.getBody();

        assertNotNull(resp);
        assertFalse(resp.usedPlaceholderBytes);
        assertEquals(5, resp.contributions.size());
        assertEquals(0.0, resp.totalTacticNetworkingDeltaUsd);
    }

    @Test
    void calculateTacticContributions_retryBranchAndLowCostLabel() {
        TacticsContributionController.TacticContributionRequest req = new TacticsContributionController.TacticContributionRequest();
        req.baseRps = 1;
        req.protoResponseSizeEffectiveBytes = 10;
        req.retryEnabled = true;
        req.retryErrorRatePct = 5.0;

        ResponseEntity<TacticsContributionController.TacticContributionResponse> response = controller.calculateTacticContributions(req);
        TacticsContributionController.TacticContributionResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(1, resp.contributions.size());
        assertEquals("< +$0.01/mo", resp.contributions.get(0).costDisplayLabel);
    }

    @Test
    void calculateTacticContributions_tlsAndMtlsBranches() {
        TacticsContributionController.TacticContributionRequest req1 = new TacticsContributionController.TacticContributionRequest();
        req1.baseRps = 5000;
        req1.protoResponseSizeEffectiveBytes = 0;
        req1.tlsEnabled = true;
        req1.tlsReconnectsPerHour = 3600;
        req1.tlsOverheadBytesFromBackend = 50;

        ResponseEntity<TacticsContributionController.TacticContributionResponse> response1 = controller.calculateTacticContributions(req1);
        assertNotNull(response1.getBody());
        assertTrue(response1.getBody().usedPlaceholderBytes);
        assertEquals(1, response1.getBody().contributions.size());

        TacticsContributionController.TacticContributionRequest req2 = new TacticsContributionController.TacticContributionRequest();
        req2.baseRps = 1000;
        req2.mtlsEnabled = true;
        req2.tlsReconnectsPerHour = 0;
        req2.tlsOverheadBytesFromBackend = 0;

        ResponseEntity<TacticsContributionController.TacticContributionResponse> response2 = controller.calculateTacticContributions(req2);
        assertNotNull(response2.getBody());
        assertEquals("bytes", response2.getBody().contributions.get(0).kind);
    }

    @Test
    void calculateTacticContributions_oauthLocalAndRemoteModes() {
        TacticsContributionController.TacticContributionRequest req1 = new TacticsContributionController.TacticContributionRequest();
        req1.baseRps = 1000;
        req1.protoResponseSizeEffectiveBytes = 1000;
        req1.oauthEnabled = true;
        req1.tokenValidationMode = "LOCAL";
        req1.tokenTtlSeconds = 0;
        req1.concurrentClients = 0;
        req1.jwtOverheadBytesFromBackend = 0;

        ResponseEntity<TacticsContributionController.TacticContributionResponse> response1 = controller.calculateTacticContributions(req1);
        assertNotNull(response1.getBody());
        assertEquals(0.0, response1.getBody().contributions.get(0).estimatedMonthlyCostUsd);

        TacticsContributionController.TacticContributionRequest req2 = new TacticsContributionController.TacticContributionRequest();
        req2.baseRps = 200000;
        req2.protoResponseSizeEffectiveBytes = 2000;
        req2.oauthEnabled = true;
        req2.tokenValidationMode = "REMOTE_INTROSPECTION";
        req2.tokenTtlSeconds = 60;
        req2.concurrentClients = 2;
        req2.jwtOverheadBytesFromBackend = 800;

        ResponseEntity<TacticsContributionController.TacticContributionResponse> response2 = controller.calculateTacticContributions(req2);
        assertNotNull(response2.getBody());
        assertTrue(response2.getBody().totalTacticNetworkingDeltaUsd > 0);
    }

    @Test
    void calculateTacticContributions_allEgressPricingTiers() {
        TacticsContributionController.TacticContributionRequest req = new TacticsContributionController.TacticContributionRequest();
        req.baseRps = 5_000_000;
        req.protoResponseSizeEffectiveBytes = 5_000;
        req.retryEnabled = true;
        req.retryErrorRatePct = 50.0;

        ResponseEntity<TacticsContributionController.TacticContributionResponse> response = controller.calculateTacticContributions(req);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().totalTacticNetworkingDeltaUsd > 0);
    }

    @Test
    void calculateTacticContributions_zeroEgressCost() {
        TacticsContributionController.TacticContributionRequest req = new TacticsContributionController.TacticContributionRequest();
        req.baseRps = 0;
        req.protoResponseSizeEffectiveBytes = -100;
        req.retryEnabled = true;

        ResponseEntity<TacticsContributionController.TacticContributionResponse> response = controller.calculateTacticContributions(req);
        assertNotNull(response.getBody());
        assertEquals(0.0, response.getBody().totalTacticNetworkingDeltaUsd);
    }
}