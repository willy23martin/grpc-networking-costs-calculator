package com.calculator.infrastructure.web.rest;

import com.calculator.domain.repository.cloud.reliability.CloudReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.resiliency.CloudResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.resiliency.ResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UnitEconomicsControllerTest {

    @Mock
    SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;
    @Mock
    CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;

    @Mock
    ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository;
    @Mock
    CloudReliabilityArchitecturalDecisionRepository cloudReliabilityArchitecturalDecisionRepository;

    @Mock
    ResiliencyArchitecturalDecisionRepository resiliencyArchitecturalDecisionRepository;
    @Mock
    CloudResiliencyArchitecturalDecisionRepository cloudResiliencyArchitecturalDecisionRepository;

    @InjectMocks
    private UnitEconomicsController controller;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(reliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(cloudReliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions()).thenReturn(Collections.emptyList());

        Mockito.lenient().when(resiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(cloudResiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions()).thenReturn(Collections.emptyList());

        Mockito.lenient().when(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());
    }

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
        assertEquals(150.0, resp1.finopsSavingUsd, 0.01);
        assertEquals(835.0, resp1.grossCloudInfraCostUsd, 0.01); // FIXED: The baseline infrastructure math totals 835.0
        assertEquals(685.0, resp1.netCloudInfraCostUsd, 0.01);   // FIXED: 835.0 - 150.0 = 685.0
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
    }
}