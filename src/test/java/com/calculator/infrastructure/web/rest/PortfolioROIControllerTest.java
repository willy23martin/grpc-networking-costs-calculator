package com.calculator.infrastructure.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PortfolioROIControllerTest {

    private final PortfolioROIController controller = new PortfolioROIController();

    @Test
    void calculatePortfolioRoi_emptyServices() {
        PortfolioROIController.PortfolioRoiRequest req = new PortfolioROIController.PortfolioRoiRequest();
        req.services = null;
        req.finopsMonthlySaving = 100.0;
        req.ec2BaselineSpend = 500.0;

        ResponseEntity<PortfolioROIController.PortfolioRoiResponse> response = controller.calculatePortfolioRoi(req);
        PortfolioROIController.PortfolioRoiResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(0, resp.serviceCount);
        assertFalse(resp.hasRevenueData);
        assertEquals("No services in portfolio.", resp.note);
    }

    @Test
    void calculatePortfolioRoi_withRevenueAndFinOps() {
        PortfolioROIController.PortfolioRoiRequest req = new PortfolioROIController.PortfolioRoiRequest();
        req.finopsMonthlySaving = 200.0;
        req.ec2BaselineSpend = 1000.0;
        req.services = new ArrayList<>();

        PortfolioROIController.ServiceEntry s1 = new PortfolioROIController.ServiceEntry();
        s1.name = "Service A";
        s1.buc = "BUC001";
        s1.tco = 1000.0;
        s1.revenuePerUserMonth = 10.0;
        s1.consumers = 500;
        s1.rps = 50;
        req.services.add(s1);

        PortfolioROIController.ServiceEntry s2 = new PortfolioROIController.ServiceEntry();
        s2.name = "Service B";
        s2.buc = "BUC002";
        s2.tco = 2000.0;
        s2.revenuePerUserMonth = 5.0;
        s2.consumers = 1000;
        s2.rps = 150;
        req.services.add(s2);

        ResponseEntity<PortfolioROIController.PortfolioRoiResponse> response = controller.calculatePortfolioRoi(req);
        PortfolioROIController.PortfolioRoiResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(2, resp.serviceCount);
        assertTrue(resp.hasRevenueData);
        assertEquals(3000.0, resp.totalMonthlyTco);
        assertEquals(10000.0, resp.totalMonthlyRevenue);
        assertEquals(200, resp.totalRps);
        assertEquals(1500, resp.totalConsumers);
        assertTrue(resp.portfolioArpu > 0);
        assertTrue(resp.monthlyRoi > 0);
        assertTrue(resp.annualRoi > 0);
        assertTrue(resp.breakEvenUsers > 0);
        assertTrue(resp.costPerRequestUsd > 0);
        assertTrue(resp.costPerUserPerMonth > 0);
        assertTrue(resp.costPerUserPerDay > 0);
        assertTrue(resp.finopsAdjustedRoi > 0);
        assertTrue(resp.finopsRoiImprovementPct > 0);
        assertEquals(2, resp.serviceBreakdown.size());
    }

    @Test
    void calculatePortfolioRoi_noRevenueDataAndZeroTotals() {
        PortfolioROIController.PortfolioRoiRequest req = new PortfolioROIController.PortfolioRoiRequest();
        req.finopsMonthlySaving = -50.0;
        req.services = new ArrayList<>();

        PortfolioROIController.ServiceEntry s1 = new PortfolioROIController.ServiceEntry();
        s1.name = "Free Service";
        s1.tco = 500.0;
        s1.revenuePerUserMonth = 0.0;
        s1.consumers = 100;
        s1.rps = 0;
        req.services.add(s1);

        ResponseEntity<PortfolioROIController.PortfolioRoiResponse> response = controller.calculatePortfolioRoi(req);
        PortfolioROIController.PortfolioRoiResponse resp = response.getBody();

        assertNotNull(resp);
        assertFalse(resp.hasRevenueData);
        assertEquals(0.0, resp.monthlyRoi);
        assertEquals(0.0, resp.annualRoi);
        assertEquals(0.0, resp.revenuePerDollarInfra);
        assertEquals(0, resp.breakEvenUsers);
        assertEquals(0.0, resp.costPerRequestUsd);
        assertEquals(500.0, resp.finopsAdjustedTco);
        assertEquals(0.0, resp.finopsAdjustedRoi);
    }

    @Test
    void calculatePortfolioRoi_negativeAndEdgeValues() {
        PortfolioROIController.PortfolioRoiRequest req = new PortfolioROIController.PortfolioRoiRequest();
        req.services = new ArrayList<>();

        PortfolioROIController.ServiceEntry s1 = new PortfolioROIController.ServiceEntry();
        s1.name = "Invalid Values Service";
        s1.tco = -100.0;
        s1.revenuePerUserMonth = -5.0;
        s1.consumers = -10;
        s1.rps = -20;
        req.services.add(s1);

        ResponseEntity<PortfolioROIController.PortfolioRoiResponse> response = controller.calculatePortfolioRoi(req);
        PortfolioROIController.PortfolioRoiResponse resp = response.getBody();

        assertNotNull(resp);
        assertEquals(0.0, resp.totalMonthlyTco);
        assertEquals(0.0, resp.totalMonthlyRevenue);
        assertEquals(0, resp.totalRps);
        assertEquals(0, resp.totalConsumers);
        assertEquals(0.0, resp.portfolioArpu);
        assertEquals(0.0, resp.costPerUserPerMonth);
    }
}