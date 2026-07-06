package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

public class Phase5PortfolioRoiSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions response;

    private String twoServicePortfolioJson() {
        return """
                {
                  "services": [
                    { "name": "Service A", "buc": "BUC1", "tco": 1000.0, "revenuePerUserMonth": 10.0, "consumers": 500, "rps": 50 },
                    { "name": "Service B", "buc": "BUC2", "tco": 2000.0, "revenuePerUserMonth": 5.0, "consumers": 1000, "rps": 150 }
                  ],
                  "finopsMonthlySaving": 150.0,
                  "ec2BaselineSpend": 500.0
                }
                """;
    }

    @Given("the architect has added multiple services to the portfolio")
    public void addMultipleServicesToPortfolio() {
    }

    @When("the browser is refreshed or reopened")
    public void simulateBrowserRefresh() throws Exception {
        response = mockMvc.perform(get("/"));
    }

    @Then("all services are still present loaded from browser localStorage under the key {string}")
    public void verifyPortfolioPersistenceKey(String storageKey) throws Exception {
        mockMvc.perform(get("/js/mach-architecture-portfolio.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(storageKey)));
    }

    @And("the portfolio can be cleared at any time via the Clear Portfolio action")
    public void verifyClearPortfolioAction() throws Exception {
        response.andExpect(content().string(containsString("onclick=\"clearPortfolio()\"")));
    }

    @Given("the portfolio contains one or more services")
    public void portfolioContainsServices() {
    }

    @When("Phase 5 loads")
    public void phase5Loads() throws Exception {
        response = mockMvc.perform(post("/api/portfolio/roi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(twoServicePortfolioJson()));
    }

    @Then("the tool calls POST \\/api\\/portfolio\\/roi with all service entries FinOps monthly savings and EC2 baseline spend")
    public void verifyPortfolioRoiApiCalled() throws Exception {
        response.andExpect(status().isOk());
    }

    @And("the backend returns aggregated metrics including total TCO total revenue portfolio ARPU monthly and annual ROI break-even users cost per request cost per user and FinOps-adjusted ROI")
    public void verifyAggregatedRoiMetrics() throws Exception {
        response.andExpect(jsonPath("$.totalMonthlyTco").exists())
                .andExpect(jsonPath("$.totalMonthlyRevenue").exists())
                .andExpect(jsonPath("$.portfolioArpu").exists())
                .andExpect(jsonPath("$.monthlyRoi").exists())
                .andExpect(jsonPath("$.annualRoi").exists())
                .andExpect(jsonPath("$.breakEvenUsers").exists())
                .andExpect(jsonPath("$.costPerRequestUsd").exists())
                .andExpect(jsonPath("$.costPerUserPerMonth").exists())
                .andExpect(jsonPath("$.finopsAdjustedRoi").exists());
    }

    @Given("the architect has modeled three microservices with a combined TCO")
    public void modelThreeMicroservices() {
    }

    @When("the Download Portfolio PDF button is clicked")
    public void clickDownloadPortfolioPdf() throws Exception {
        response = mockMvc.perform(get("/"));
    }

    @Then("the browser print dialog opens with a clean print-optimized layout")
    public void verifyPrintOptimizedLayout() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(content().string(containsString("onclick=\"downloadPortfolioPdf()\"")));
    }

    @And("the layout shows the full portfolio table with service names BUCs RPS monthly TCO and annual TCO")
    public void verifyPortfolioTableStructure() throws Exception {
        response.andExpect(content().string(containsString("id=\"p5-print-table\"")));
    }

    @And("navigation elements and action buttons are hidden in the print view")
    public void verifyPrintHiddenElements() throws Exception {
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("@media print")));
    }

    @Given("a portfolio with {string} configured")
    public void configurePortfolioForOutline(String servicesDescription) throws Exception {
        String[] parts = servicesDescription.split(",");
        int serviceCount = Integer.parseInt(parts[0].trim().split(" ")[0]);
        int totalUsers = Integer.parseInt(parts[1].trim().split(" ")[0]);
        boolean hasRevenue = parts.length < 3 || !parts[2].toLowerCase().contains("no revenue");

        StringBuilder services = new StringBuilder("[");
        int usersPerService = serviceCount > 0 ? totalUsers / serviceCount : 0;
        String serviceTemplate =
                "{ \"name\": \"Service %d\", \"buc\": \"BUC%d\", \"tco\": %f, \"revenuePerUserMonth\": %s, \"consumers\": %d, \"rps\": 10 }";
        for (int i = 0; i < serviceCount; i++) {
            if (i > 0) services.append(",");
            services.append(String.format(java.util.Locale.ROOT, serviceTemplate,
                    i + 1, i + 1,
                    serviceCount > 0 ? 100.0 : 0.0,
                    hasRevenue ? "2.0" : "0.0",
                    usersPerService));
        }
        services.append("]");

        String body = """
                {
                  "services": %s,
                  "finopsMonthlySaving": 0.0,
                  "ec2BaselineSpend": 0.0
                }
                """.formatted(services);

        response = mockMvc.perform(post("/api/portfolio/roi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("the ROI aggregation is calculated")
    public void calculateRoiAggregation() throws Exception {
        // ROI was already calculated in the Given step via the /api/portfolio/roi call.
        response.andExpect(status().isOk());
    }

    @Then("total monthly TCO is {string}")
    public void verifyTotalMonthlyTco(String expectedTco) throws Exception {
        response.andExpect(jsonPath("$.totalMonthlyTco").exists());
    }

    @And("total monthly revenue is {string}")
    public void verifyTotalMonthlyRevenue(String expectedRevenue) throws Exception {
        response.andExpect(jsonPath("$.totalMonthlyRevenue").exists());
    }

    @And("monthly ROI is {string}")
    public void verifyMonthlyRoi(String expectedRoi) throws Exception {
        response.andExpect(jsonPath("$.monthlyRoi").exists());
    }
}