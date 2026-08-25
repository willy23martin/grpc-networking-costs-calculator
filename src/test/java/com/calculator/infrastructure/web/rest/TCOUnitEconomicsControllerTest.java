package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.CostEfficiencyCalculator;
import com.calculator.domain.dto.requests.TCOUnitEconomicsRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestRepositoryStubsConfiguration.class)
class TCOUnitEconomicsControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CostEfficiencyCalculator costEfficiencyCalculator;

    @Test
    void calculateCloudInfraTotal_allScenarios() throws Exception {
        final com.calculator.domain.dto.requests.CloudInfrastructureTotalCostRequest req1 =
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
        final TCOUnitEconomicsRequest req = new TCOUnitEconomicsRequest();
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