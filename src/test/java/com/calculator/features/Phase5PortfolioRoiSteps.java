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
public class Phase5PortfolioRoiSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions response;

    // ── Portfolio persistence scenario ────────────────────────────────────────

    @Given("the architect has added multiple services to the portfolio")
    public void addMultipleServicesToPortfolio() throws Exception {
        // Add two representative services via the portfolio API.
        mockMvc.perform(post("/api/portfolio/add").param("bucId", "BUC1"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/portfolio/add").param("bucId", "BUC2"))
                .andExpect(status().isOk());
    }

    @When("the browser is refreshed or reopened")
    public void simulateBrowserRefresh() throws Exception {
        // The browser reload is simulated by fetching /calculator without session context,
        // representing the portfolio being read back from localStorage on page init.
        response = mockMvc.perform(get("/calculator"));
    }

    @Then("all services are still present loaded from browser localStorage under the key {string}")
    public void verifyPortfolioPersistenceKey(String storageKey) throws Exception {
        // The calculator HTML must include the localStorage key constant so the frontend
        // can read the portfolio on page load.
        response.andExpect(status().isOk())
                .andExpect(content().string(containsString(storageKey)));
    }

    @And("the portfolio can be cleared at any time via the Clear Portfolio action")
    public void verifyClearPortfolioAction() throws Exception {
        response.andExpect(content().string(containsString("id=\"clear-portfolio\"")));
    }

    // ── Backend ROI computation scenario ─────────────────────────────────────

    @Given("the portfolio contains one or more services")
    public void portfolioContainsServices() throws Exception {
        mockMvc.perform(post("/api/portfolio/add").param("bucId", "BUC1"))
                .andExpect(status().isOk());
    }

    @When("Phase 5 loads")
    public void phase5Loads() throws Exception {
        response = mockMvc.perform(post("/api/portfolio/roi")
                .param("finOpsMonthlySavings", "150")
                .param("ec2BaselineSpend", "500"));
    }

    @Then("the tool calls POST \\/api\\/portfolio\\/roi with all service entries FinOps monthly savings and EC2 baseline spend")
    public void verifyPortfolioRoiApiCalled() throws Exception {
        response.andExpect(status().isOk());
    }

    @And("the backend returns aggregated metrics including total TCO total revenue portfolio ARPU monthly and annual ROI break-even users cost per request cost per user and FinOps-adjusted ROI")
    public void verifyAggregatedRoiMetrics() throws Exception {
        response.andExpect(jsonPath("$.totalTco").exists())
                .andExpect(jsonPath("$.totalRevenue").exists())
                .andExpect(jsonPath("$.portfolioArpu").exists())
                .andExpect(jsonPath("$.monthlyRoi").exists())
                .andExpect(jsonPath("$.annualRoi").exists())
                .andExpect(jsonPath("$.breakEvenUsers").exists())
                .andExpect(jsonPath("$.costPerRequest").exists())
                .andExpect(jsonPath("$.costPerUser").exists())
                .andExpect(jsonPath("$.finOpsAdjustedRoi").exists());
    }

    // ── PDF download scenario ─────────────────────────────────────────────────

    @Given("the architect has modeled three microservices with a combined TCO")
    public void modelThreeMicroservices() throws Exception {
        mockMvc.perform(post("/api/portfolio/add").param("bucId", "BUC1")).andExpect(status().isOk());
        mockMvc.perform(post("/api/portfolio/add").param("bucId", "BUC2")).andExpect(status().isOk());
        mockMvc.perform(post("/api/portfolio/add").param("bucId", "BUC3")).andExpect(status().isOk());
    }

    @When("the Download Portfolio PDF button is clicked")
    public void clickDownloadPortfolioPdf() throws Exception {
        response = mockMvc.perform(get("/calculator"));
    }

    @Then("the browser print dialog opens with a clean print-optimized layout")
    public void verifyPrintOptimizedLayout() throws Exception {
        // The calculator.html must include a print-specific stylesheet or media query
        // and the download button that triggers window.print().
        response.andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"download-portfolio-pdf\"")));
    }

    @And("the layout shows the full portfolio table with service names BUCs RPS monthly TCO and annual TCO")
    public void verifyPortfolioTableStructure() throws Exception {
        response.andExpect(content().string(containsString("id=\"portfolio-table\"")));
    }

    @And("navigation elements and action buttons are hidden in the print view")
    public void verifyPrintHiddenElements() throws Exception {
        // The print stylesheet hides nav and action elements; verified by the presence
        // of the print-hide CSS class or a @media print rule reference in the HTML.
        response.andExpect(content().string(containsString("print-hide")));
    }

    // ── Unit economics Outline steps ──────────────────────────────────────────

    @Given("a portfolio with {string} configured")
    public void configurePortfolioForOutline(String servicesDescription) throws Exception {
        // servicesDescription e.g. "2 services, 10000 users"
        // Parsing is simplified; real implementation would build the payload from the description.
        response = mockMvc.perform(post("/api/portfolio/roi")
                .param("servicesDescription", servicesDescription)
                .param("finOpsMonthlySavings", "0")
                .param("ec2BaselineSpend", "0"));
    }

    @When("the ROI aggregation is calculated")
    public void calculateRoiAggregation() throws Exception {
        // ROI was already calculated in the Given step via the /api/portfolio/roi call.
        response.andExpect(status().isOk());
    }

    @Then("total monthly TCO is {string}")
    public void verifyTotalMonthlyTco(String expectedTco) throws Exception {
        response.andExpect(jsonPath("$.totalTco").value(expectedTco));
    }

    @And("total monthly revenue is {string}")
    public void verifyTotalMonthlyRevenue(String expectedRevenue) throws Exception {
        response.andExpect(jsonPath("$.totalRevenue").value(expectedRevenue));
    }

    @And("monthly ROI is {string}")
    public void verifyMonthlyRoi(String expectedRoi) throws Exception {
        response.andExpect(jsonPath("$.monthlyRoi").value(expectedRoi));
    }
}
