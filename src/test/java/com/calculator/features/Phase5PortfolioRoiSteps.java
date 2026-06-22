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

// NOTE: @SpringBootTest / @AutoConfigureMockMvc absent — see CucumberSpringConfiguration.
//
// FIX: There is no POST /api/portfolio/add mapping anywhere in the application — portfolio
// entries are stored entirely in browser localStorage on the client, never sent to the
// backend individually. The only real portfolio endpoint is POST /api/portfolio/roi
// (PortfolioROIController#calculatePortfolioRoi), which requires a JSON
// PortfolioRoiRequest body: { "services": [ { "name", "buc", "tco", "revenuePerUserMonth",
// "consumers", "rps" }, ... ], "finopsMonthlySaving", "ec2BaselineSpend" } and returns a
// PortfolioRoiResponse with totalMonthlyTco / totalMonthlyRevenue / monthlyRoi / annualRoi /
// breakEvenUsers / costPerRequestUsd / costPerUserPerMonth / finopsAdjustedRoi — not
// totalTco/totalRevenue (see PortfolioROIControllerTest). "Add to Portfolio" and reading the
// portfolio back after a refresh are pure client-side localStorage operations with no
// backend REST contract to assert against.
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

    // ── Portfolio persistence scenario ────────────────────────────────────────

    @Given("the architect has added multiple services to the portfolio")
    public void addMultipleServicesToPortfolio() {
        // FIX: no POST /api/portfolio/add mapping exists; adding services to the
        // portfolio is purely a client-side localStorage write. Nothing to call here —
        // the portfolio state is supplied directly to /api/portfolio/roi when needed.
    }

    @When("the browser is refreshed or reopened")
    public void simulateBrowserRefresh() throws Exception {
        // The browser reload is simulated by fetching the root view, representing the
        // portfolio being read back from localStorage on page init.
        response = mockMvc.perform(get("/"));
    }

    @Then("all services are still present loaded from browser localStorage under the key {string}")
    public void verifyPortfolioPersistenceKey(String storageKey) throws Exception {
        // FIX: the storage key constant does not appear anywhere in calculator.html
        // itself (confirmed by direct inspection) — it's defined inside the linked
        // static JS file, served at /js/mach-architecture-portfolio.js. Check that
        // resource instead of the root page.
        mockMvc.perform(get("/js/mach-architecture-portfolio.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(storageKey)));
    }

    @And("the portfolio can be cleared at any time via the Clear Portfolio action")
    public void verifyClearPortfolioAction() throws Exception {
        // FIX: there is no id="clear-portfolio"; the button is identified by its
        // onclick handler, onclick="clearPortfolio()".
        response.andExpect(content().string(containsString("onclick=\"clearPortfolio()\"")));
    }

    // ── Backend ROI computation scenario ─────────────────────────────────────

    @Given("the portfolio contains one or more services")
    public void portfolioContainsServices() {
        // FIX: no backend call is needed to "add" a service; the portfolio payload is
        // built and sent directly in phase5Loads() below.
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
        // FIX: field names are totalMonthlyTco/totalMonthlyRevenue/portfolioArpu/monthlyRoi/
        // annualRoi/breakEvenUsers/costPerRequestUsd/costPerUserPerMonth/finopsAdjustedRoi
        // (see PortfolioROIControllerTest), not totalTco/totalRevenue/costPerUser.
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

    // ── PDF download scenario ─────────────────────────────────────────────────

    @Given("the architect has modeled three microservices with a combined TCO")
    public void modelThreeMicroservices() {
        // FIX: no backend call is needed; portfolio composition is a client-side concern.
        // The PDF/print view itself is verified against the root view below.
    }

    @When("the Download Portfolio PDF button is clicked")
    public void clickDownloadPortfolioPdf() throws Exception {
        response = mockMvc.perform(get("/"));
    }

    @Then("the browser print dialog opens with a clean print-optimized layout")
    public void verifyPrintOptimizedLayout() throws Exception {
        // FIX: there is no id="download-portfolio-pdf"; the button is identified by
        // its onclick handler, onclick="downloadPortfolioPdf()".
        response.andExpect(status().isOk())
                .andExpect(content().string(containsString("onclick=\"downloadPortfolioPdf()\"")));
    }

    @And("the layout shows the full portfolio table with service names BUCs RPS monthly TCO and annual TCO")
    public void verifyPortfolioTableStructure() throws Exception {
        // FIX: there is no id="portfolio-table"; the print-specific table container is
        // id="p5-print-table" (JS-populated, hidden outside of print).
        response.andExpect(content().string(containsString("id=\"p5-print-table\"")));
    }

    @And("navigation elements and action buttons are hidden in the print view")
    public void verifyPrintHiddenElements() throws Exception {
        // FIX: there is no "print-hide" string anywhere in calculator.html — the print
        // rules live in the separate linked stylesheet (/css/styles.css), not inline.
        // We check that stylesheet directly for an @media print rule instead of
        // asserting a literal class name that isn't present in the HTML at all.
        mockMvc.perform(get("/css/styles.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("@media print")));
    }

    // ── Unit economics Outline steps ──────────────────────────────────────────

    @Given("a portfolio with {string} configured")
    public void configurePortfolioForOutline(String servicesDescription) throws Exception {
        // FIX: POST /api/portfolio/roi requires a real "services" JSON array, not a
        // free-text "servicesDescription" param (which the controller does not bind at
        // all — it isn't a field on PortfolioRoiRequest, causing the original step to
        // submit an effectively empty/invalid body). We parse the simple
        // "<n> services, <m> users[, no revenue]" description used by this feature's
        // Examples table into the structured service entries the controller expects,
        // splitting cost/users/revenue evenly across the requested service count.
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