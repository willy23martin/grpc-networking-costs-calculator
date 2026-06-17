package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

// NOTE: @SpringBootTest / @AutoConfigureMockMvc absent — see CucumberSpringConfiguration.
public class Phase4TcoReportSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions response;

    // ── Proto submission triggers TCO calculation ─────────────────────────────

    @Given("a BUC has been selected and a proto file is loaded")
    public void bucSelectedAndProtoLoaded() throws Exception {
        mockMvc.perform(post("/api/session/buc")
                .param("bucId", "BUC1"))
                .andExpect(status().isOk());
    }

    @When("the architect clicks Calculate TCO in Phase 3")
    public void clickCalculateTco() throws Exception {
        mockMvc.perform(post("/api/session/tactics")
                .param("tactic", "retry")
                .param("baseRps", "1000")
                .param("errorRate", "5%"))
                .andExpect(status().isOk());

        response = mockMvc.perform(post("/calculateTCO")
                .param("bucId", "BUC1"));
    }

    @Then("the tool calls POST \\/api\\/session\\/tactics and POST \\/calculateTCO")
    public void verifyTwoApiCallsExecuted() throws Exception {
        // Both calls were made in the When step; verify the final response is OK.
        response.andExpect(status().isOk());
    }

    @And("the system parses the Spring Boot Thymeleaf HTML response to extract the cost model parameters")
    public void verifyCostModelParametersExtracted() throws Exception {
        response.andExpect(jsonPath("$.protoSizes").exists())
                .andExpect(jsonPath("$.effectiveSizes").exists())
                .andExpect(jsonPath("$.monthlyVolume").exists())
                .andExpect(jsonPath("$.dataTransferCost").exists())
                .andExpect(jsonPath("$.rpsAdjustments").exists())
                .andExpect(jsonPath("$.securityOverheadBytes").exists());
    }

    @And("the interface automatically navigates to Phase 4 to render the full report")
    public void verifyNavigationToPhase4() throws Exception {
        // Phase 4 is rendered as a section in calculator.html with id="phase4".
        mockMvc.perform(get("/calculator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"phase4\"")));
    }

    // ── Phase 4 report renders complete breakdown ─────────────────────────────

    @Given("the backend has returned a valid TCO response")
    public void backendReturnedValidTcoResponse() throws Exception {
        response = mockMvc.perform(post("/calculateTCO")
                .param("bucId", "BUC1"));
        response.andExpect(status().isOk());
    }

    @When("Phase 4 loads")
    public void phase4Loads() throws Exception {
        response = mockMvc.perform(get("/calculator"));
        response.andExpect(status().isOk());
    }

    @Then("the report displays a Service Identity Banner with metadata and active tactics")
    public void verifyServiceIdentityBanner() throws Exception {
        response.andExpect(content().string(containsString("id=\"service-identity-banner\"")));
    }

    @And("the report displays an RPS Adjustment Banner showing load shifts")
    public void verifyRpsAdjustmentBanner() throws Exception {
        response.andExpect(content().string(containsString("id=\"rps-adjustment-banner\"")));
    }

    @And("the report displays a Security Overhead Banner showing RFC byte additions")
    public void verifySecurityOverheadBanner() throws Exception {
        response.andExpect(content().string(containsString("id=\"security-overhead-banner\"")));
    }

    @And("the report displays Tactics Summary Tables")
    public void verifyTacticsSummaryTables() throws Exception {
        response.andExpect(content().string(containsString("id=\"tactics-summary\"")));
    }

    @And("the page renders a Proto Sizes Table with volumes throughput and AWS egress charges")
    public void verifyProtoSizesTable() throws Exception {
        response.andExpect(content().string(containsString("id=\"proto-sizes-table\"")));
    }

    @And("the page renders a complete TCO Breakdown Table with networking cloud infra FinOps metrics and DR/BC status")
    public void verifyTcoBreakdownTable() throws Exception {
        response.andExpect(content().string(containsString("id=\"tco-breakdown-table\"")));
    }

    @And("the page renders Unit Economics breakdown ROI Analysis panel and FinOps Architecture Notes")
    public void verifyUnitEconomicsAndRoiPanel() throws Exception {
        response.andExpect(content().string(containsString("id=\"unit-economics\"")))
                .andExpect(content().string(containsString("id=\"roi-analysis\"")))
                .andExpect(content().string(containsString("id=\"finops-notes\"")));
    }

    // ── Save to portfolio ─────────────────────────────────────────────────────

    @Given("Phase 4 displays a non-zero TCO")
    public void phase4DisplaysNonZeroTco() throws Exception {
        mockMvc.perform(post("/calculateTCO").param("bucId", "BUC1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMonthlyCost").isNotEmpty());
    }

    @When("the architect clicks Add to Portfolio")
    public void clickAddToPortfolio() throws Exception {
        response = mockMvc.perform(post("/api/portfolio/add")
                .param("bucId", "BUC1"));
    }

    @Then("the complete service entry profile is persisted to browser localStorage")
    public void verifyServicePersistedToPortfolio() throws Exception {
        // The API confirms the entry was prepared for client-side localStorage persistence.
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.portfolioEntry").exists())
                .andExpect(jsonPath("$.storageKey").value("grpc_tco_portfolio_v1"));
    }

    @And("a toast notification confirms the save making the portfolio available across page reloads")
    public void verifyToastNotification() throws Exception {
        response.andExpect(jsonPath("$.toastMessage").exists());
    }

    // ── TCO components Outline steps ──────────────────────────────────────────

    @Given("the TCO report has been generated")
    public void tcoReportGenerated() throws Exception {
        mockMvc.perform(post("/calculateTCO").param("bucId", "BUC1"))
                .andExpect(status().isOk());
    }

    @Then("the {string} is sourced from {string}")
    public void verifyTcoComponentSource(String tcoComponent, String architecturalSource) throws Exception {
        mockMvc.perform(get("/api/tco/components")
                        .param("component", tcoComponent))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.architecturalSource").value(architecturalSource));
    }

    @And("it is measured using {string} cost metric")
    public void verifyTcoCostMetricType(String costMetricType) throws Exception {
        mockMvc.perform(get("/api/tco/components"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.costMetricType").value(costMetricType));
    }
}
