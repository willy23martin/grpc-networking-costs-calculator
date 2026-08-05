package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.requests.portfolio.PortfolioUnitEconomicsRequest;
import com.calculator.domain.dto.requests.portfolio.ServiceEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestRepositoryStubsConfiguration.class)
class PortfolioUnitEconomicsControllerTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void calculatePortfolioRoi_emptyServices() throws Exception {
        PortfolioUnitEconomicsRequest req = new PortfolioUnitEconomicsRequest();
        req.services = null;
        req.finopsMonthlySaving = 100.0;
        req.ec2BaselineSpend = 500.0;

        mockMvc.perform(post("/api/portfolio/roi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceCount").value(0))
                .andExpect(jsonPath("$.hasRevenueData").value(false))
                .andExpect(jsonPath("$.note").value("No services in portfolio."));
    }

    @Test
    void calculatePortfolioRoi_withRevenueAndFinOps() throws Exception {
        PortfolioUnitEconomicsRequest req = new PortfolioUnitEconomicsRequest();
        req.finopsMonthlySaving = 200.0;
        req.ec2BaselineSpend = 1000.0;
        req.services = new ArrayList<>();

        ServiceEntry s1 = new ServiceEntry();
        s1.name = "Service A";
        s1.buc = "BUC001";
        s1.tco = 1000.0;
        s1.revenuePerUserMonth = 10.0;
        s1.consumers = 500;
        s1.rps = 50;
        req.services.add(s1);

        ServiceEntry s2 = new ServiceEntry();
        s2.name = "Service B";
        s2.buc = "BUC002";
        s2.tco = 2000.0;
        s2.revenuePerUserMonth = 5.0;
        s2.consumers = 1000;
        s2.rps = 150;
        req.services.add(s2);

        mockMvc.perform(post("/api/portfolio/roi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceCount").value(2))
                .andExpect(jsonPath("$.hasRevenueData").value(true))
                .andExpect(jsonPath("$.totalMonthlyTco").value(3000.0))
                .andExpect(jsonPath("$.totalMonthlyRevenue").value(10000.0))
                .andExpect(jsonPath("$.totalRps").value(200))
                .andExpect(jsonPath("$.totalConsumers").value(1500))
                // Note: Fixed syntax wraps greaterThan directly within .value()
                .andExpect(jsonPath("$.portfolioArpu").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.monthlyRoi").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.annualRoi").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.breakEvenUsers").value(greaterThan(0)))
                .andExpect(jsonPath("$.costPerRequestUsd").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.costPerUserPerMonth").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.costPerUserPerDay").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.finopsAdjustedRoi").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.finopsRoiImprovementPct").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.serviceBreakdown").isArray())
                .andExpect(jsonPath("$.serviceBreakdown.length()").value(2));
    }

    @Test
    void calculatePortfolioRoi_noRevenueDataAndZeroTotals() throws Exception {
        PortfolioUnitEconomicsRequest req = new PortfolioUnitEconomicsRequest();
        req.finopsMonthlySaving = -50.0;
        req.services = new ArrayList<>();

        ServiceEntry s1 = new ServiceEntry();
        s1.name = "Free Service";
        s1.tco = 500.0;
        s1.revenuePerUserMonth = 0.0;
        s1.consumers = 100;
        s1.rps = 0;
        req.services.add(s1);

        mockMvc.perform(post("/api/portfolio/roi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasRevenueData").value(false))
                .andExpect(jsonPath("$.monthlyRoi").value(0.0))
                .andExpect(jsonPath("$.annualRoi").value(0.0))
                .andExpect(jsonPath("$.revenuePerDollarInfra").value(0.0))
                .andExpect(jsonPath("$.breakEvenUsers").value(0))
                .andExpect(jsonPath("$.costPerRequestUsd").value(0.0))
                .andExpect(jsonPath("$.finopsAdjustedTco").value(500.0))
                .andExpect(jsonPath("$.finopsAdjustedRoi").value(0.0));
    }

    @Test
    void calculatePortfolioRoi_negativeAndEdgeValues() throws Exception {
        PortfolioUnitEconomicsRequest req = new PortfolioUnitEconomicsRequest();
        req.services = new ArrayList<>();

        ServiceEntry s1 = new ServiceEntry();
        s1.name = "Invalid Values Service";
        s1.tco = -100.0;
        s1.revenuePerUserMonth = -5.0;
        s1.consumers = -10;
        s1.rps = -20;
        req.services.add(s1);

        mockMvc.perform(post("/api/portfolio/roi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonthlyTco").value(0.0))
                .andExpect(jsonPath("$.totalMonthlyRevenue").value(0.0))
                .andExpect(jsonPath("$.totalRps").value(0))
                .andExpect(jsonPath("$.totalConsumers").value(0))
                .andExpect(jsonPath("$.portfolioArpu").value(0.0))
                .andExpect(jsonPath("$.costPerUserPerMonth").value(0.0));
    }
}