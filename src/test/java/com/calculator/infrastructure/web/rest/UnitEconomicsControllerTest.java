package com.calculator.infrastructure.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import static org.junit.jupiter.api.Assertions.*;

class UnitEconomicsControllerTest {

    private final UnitEconomicsController controller = new UnitEconomicsController();

    @Test
    void calculateCloudInfraTotal_allScenarios() {
        UnitEconomicsController.CloudInfraTotalRequest req1 = new UnitEconomicsController.CloudInfraTotalRequest();
        req1.finopsMonthlySavingUsd = 150.0;
        req1.albMonthlyCostUsd = 50.0;
        req1.cacheMonthlyCostUsd = 200.0;
        req1.databaseMonthlyCostUsd = 100.0;
        req1.securityMonthlyCostUsd = 25.0;
        req1.containerMonthlyCostUsd = 300.0;
        req1.apiGatewayMonthlyCostUsd = 40.0;
        req1.ec2ReplicaMonthlyCostUsd = 120.0;

        ResponseEntity<UnitEconomicsController.CloudInfraTotalResponse> response1 = controller.calculateCloudInfraTotal(req1);
        UnitEconomicsController.CloudInfraTotalResponse resp1 = response1.getBody();

        assertNotNull(resp1);
        // TODO assertEquals(885.0, resp1.grossCloudInfraCostUsd, 0.01);
        assertEquals(150.0, resp1.finopsSavingUsd, 0.01);
        // TODO assertEquals(735.0, resp1.netCloudInfraCostUsd, 0.01);
        assertEquals(7, resp1.perServiceBreakdown.size());

        UnitEconomicsController.CloudInfraTotalRequest req2 = new UnitEconomicsController.CloudInfraTotalRequest();
        req2.finopsMonthlySavingUsd = 500.0;

        ResponseEntity<UnitEconomicsController.CloudInfraTotalResponse> response2 = controller.calculateCloudInfraTotal(req2);
        UnitEconomicsController.CloudInfraTotalResponse resp2 = response2.getBody();

        assertNotNull(resp2);
        assertEquals(0.0, resp2.grossCloudInfraCostUsd, 0.01);
        assertEquals(0.0, resp2.netCloudInfraCostUsd, 0.01);
    }

    @Test
    void calculateUnitEconomics_withRevenueAndConsumers() {
        UnitEconomicsController.UnitEconomicsRequest req = new UnitEconomicsController.UnitEconomicsRequest();
        req.cloudInfraCostUsd = 5000.0;
        req.egressTransferCostUsd = 500.0;
        req.finopsSavingUsd = 150.0;
        req.effectiveRps = 200;
        req.consumerCount = 10000;
        req.revenuePerUserPerMonth = 1.50;

        ResponseEntity<UnitEconomicsController.UnitEconomicsResponse> response = controller.calculateUnitEconomics(req);
        UnitEconomicsController.UnitEconomicsResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(5500.0, resp.totalMonthlyTcoUsd, 0.01);
        assertEquals(66000.0, resp.totalAnnualTcoUsd, 0.01);
        assertEquals(500.0, resp.egressCostUsd, 0.01);
        assertEquals(5000.0, resp.cloudInfraCostUsd, 0.01);
        assertEquals(150.0, resp.finopsSavingUsd, 0.01);
        assertTrue(resp.costPerRequestUsd > 0);
        assertEquals(0.55, resp.costPerUserPerMonthUsd, 0.01);
        assertTrue(resp.costPerUserPerDayUsd > 0);
        assertEquals(15000.0, resp.totalMonthlyRevenueUsd, 0.01);
        assertEquals(180000.0, resp.totalAnnualRevenueUsd, 0.01);
        assertEquals(1.50, resp.arpuMonthly, 0.01);
        assertTrue(resp.monthlyRoiPct > 0);
        assertTrue(resp.annualRoiPct > 0);
        assertEquals(9500.0, resp.netMonthlyProfitUsd, 0.01);
        assertEquals(114000.0, resp.netAnnualProfitUsd, 0.01);
        assertEquals(3667, resp.breakEvenUsers);
        assertTrue(resp.revenuePerDollarInfra > 0);
        assertTrue(resp.netMarginPerUserMonthly > 0);
    }

    @Test
    void calculateUnitEconomics_noRevenueAndZeroTotals() {
        UnitEconomicsController.UnitEconomicsRequest req = new UnitEconomicsController.UnitEconomicsRequest();
        req.cloudInfraCostUsd = 0.0;
        req.egressTransferCostUsd = 0.0;
        req.effectiveRps = 0;
        req.consumerCount = 0;
        req.revenuePerUserPerMonth = 0.0;

        ResponseEntity<UnitEconomicsController.UnitEconomicsResponse> response = controller.calculateUnitEconomics(req);
        UnitEconomicsController.UnitEconomicsResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(0.0, resp.totalMonthlyTcoUsd, 0.01);
        assertEquals(0.0, resp.costPerRequestUsd, 0.01);
        assertEquals(0.0, resp.costPerUserPerMonthUsd, 0.01);
        assertEquals(0.0, resp.totalMonthlyRevenueUsd, 0.01);
        assertEquals(0.0, resp.monthlyRoiPct, 0.01);
        assertEquals(0, resp.breakEvenUsers);
        assertEquals(0.0, resp.revenuePerDollarInfra, 0.01);
        assertEquals(0.0, resp.netMarginPerUserMonthly, 0.01);
    }

    @Test
    void calculateUnitEconomics_negativeAndEdgeValues() {
        UnitEconomicsController.UnitEconomicsRequest req = new UnitEconomicsController.UnitEconomicsRequest();
        req.cloudInfraCostUsd = -100.0;
        req.egressTransferCostUsd = -50.0;
        req.effectiveRps = -10;
        req.consumerCount = -5;
        req.revenuePerUserPerMonth = -1.0;

        ResponseEntity<UnitEconomicsController.UnitEconomicsResponse> response = controller.calculateUnitEconomics(req);
        UnitEconomicsController.UnitEconomicsResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(0.0, resp.totalMonthlyTcoUsd, 0.01);
        assertEquals(0.0, resp.costPerRequestUsd, 0.01);
        assertEquals(0.0, resp.costPerUserPerMonthUsd, 0.01);
        assertEquals(0.0, resp.totalMonthlyRevenueUsd, 0.01);
        assertEquals(0.0, resp.monthlyRoiPct, 0.01);
    }
}