package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.requests.UnitEconomicsRequest;
import com.calculator.domain.repository.cloud.reliability.CloudReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.resiliency.CloudResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.finops.reliability.FinOpsStrategyReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.resiliency.ResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UnitEconomicsControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    @MockitoBean
    private CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;

    @MockitoBean
    private ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository;

    @MockitoBean
    private CloudReliabilityArchitecturalDecisionRepository cloudReliabilityArchitecturalDecisionRepository;

    @MockitoBean
    private ResiliencyArchitecturalDecisionRepository resiliencyArchitecturalDecisionRepository;

    @MockitoBean
    private CloudResiliencyArchitecturalDecisionRepository cloudResiliencyArchitecturalDecisionRepository;

    @MockitoBean
    private FinOpsStrategyReliabilityArchitecturalDecisionRepository finOpsStrategyReliabilityArchitecturalDecisionRepository;

    @BeforeEach
    void setUp() {
        Mockito.lenient().when(reliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(cloudReliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions()).thenReturn(Collections.emptyList());

        Mockito.lenient().when(resiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(cloudResiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions()).thenReturn(Collections.emptyList());

        Mockito.lenient().when(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());
        Mockito.lenient().when(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());

        Mockito.lenient().when(finOpsStrategyReliabilityArchitecturalDecisionRepository.getFinOpsStrategyForAWSApplicationLoadBalancer()).thenReturn(null);
    }

    @Test
    void calculateCloudInfraTotal_allScenarios() throws Exception {
        com.calculator.domain.dto.requests.CloudInfrastructureTotalCostRequest req1 =
                new com.calculator.domain.dto.requests.CloudInfrastructureTotalCostRequest();
        req1.finopsMonthlySavingUsd = 150.0;
        req1.albMonthlyCostUsd = 50.0;
        req1.cacheMonthlyCostUsd = 200.0;
        req1.databaseMonthlyCostUsd = 100.0;
        req1.securityMonthlyCostUsd = 25.0;
        req1.containerMonthlyCostUsd = 300.0;
        req1.apiGatewayMonthlyCostUsd = 40.0;
        req1.ec2ReplicaMonthlyCostUsd = 120.0;

        mockMvc.perform(post("/api/cost/cloud-infra-total")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.finopsSavingUsd").value(150.0))
                .andExpect(jsonPath("$.grossCloudInfraCostUsd").value(835.0))
                .andExpect(jsonPath("$.netCloudInfraCostUsd").value(685.0));
    }

    @Test
    void calculateUnitEconomics_noRevenueAndZeroTotals() throws Exception {
        UnitEconomicsRequest req = new UnitEconomicsRequest();
        req.cloudInfraCostUsd = 0.0;
        req.egressTransferCostUsd = 0.0;
        req.effectiveRps = 0;
        req.consumerCount = 0;
        req.revenuePerUserPerMonth = 0.0;

        mockMvc.perform(post("/api/cost/unit-economics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonthlyTcoUsd").value(0.0))
                .andExpect(jsonPath("$.costPerRequestUsd").value(0.0))
                .andExpect(jsonPath("$.breakEvenUsers").value(0));
    }
}