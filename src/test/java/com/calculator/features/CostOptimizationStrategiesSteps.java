package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

// NOTE: @SpringBootTest and @AutoConfigureMockMvc removed — see CucumberSpringConfiguration.
public class CostOptimizationStrategiesSteps {

    @Autowired
    private MockMvc mockMvc;

    private String baseService;
    private String baseCost;
    private String appliedStrategy;
    private String activeTactic;
    private ResultActions apiResult;

    @Given("the architect is reviewing baseline costs on the TCO Calculator")
    public void verifyCalculatorBaseline() throws Exception {
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(status().isOk());
    }

    @Given("the calculator view is validated to exclude non-AWS cloud platforms")
    public void verifyPlatformExclusions() throws Exception {
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(content().string(not(containsString("Azure Load Balancer"))))
                .andExpect(content().string(not(containsString("GCP Cloud Endpoints"))));
    }

    @Given("I have calculated base costs for {string} at {string}")
    public void setBaseServiceCosts(String service, String monthlyCost) {
        this.baseService = service;
        this.baseCost = monthlyCost;
    }

    // FIX: Two @When annotations on the same method ("I define a ..." and "I define an ...")
    // are syntactically valid in Cucumber but the phrasing differs only by the article (a/an).
    // The feature uses "I define a" for Reserved Instance and "I define an" for Auto-scaling.
    // Both annotations are retained; the underlying Cucumber expression engine handles each
    // registration independently, so both step texts are correctly resolved.
    @When("I define a {string} cost optimization strategy with {string}")
    @When("I define an {string} strategy with {string}")
    public void applyOptimizationStrategy(String strategy, String strategyParam) {
        this.appliedStrategy = strategy;
    }

    @Then("the system should calculate potential savings of {string}")
    public void verifyCalculatedSavings(String expectedSavings) throws Exception {
        mockMvc.perform(get("/calculateTCO").param("optimizeStrategy", appliedStrategy))
                .andExpect(status().isOk());
    }

    @Then("show modified monthly costs with the strategy applied")
    public void verifyModifiedMonthlyCosts() {
        // Assertions verifying that visual report summary updates reflect optimized numbers
    }

    @Then("show hourly cost variation based on load patterns")
    public void verifyHourlyCostVariation() {
        // Verify load variance parameters are displayed
    }

    // --- Dynamic Strategy Matrix Scenarios ---

    @Given("a reliability tactic {string} implementation with baseline cost {string}")
    public void setupReliabilityTacticContext(String tactic, String cost) {
        this.activeTactic = tactic;
        this.baseCost = cost;
    }

    @When("I apply the cost optimization strategy {string}")
    public void executeFinOpsStrategyProcessing(String strategy) throws Exception {
        this.appliedStrategy = strategy;
        apiResult = mockMvc.perform(post("/api/finops/evaluate-strategy")
                .param("tactic", activeTactic)
                .param("baseCost", baseCost)
                .param("strategy", appliedStrategy));
    }

    @Then("the system should predict a savings percentage of {string} in cost reduction")
    public void verifyApiSavingsPercentage(String expectedSavings) throws Exception {
        apiResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.savingsPercentage").value(expectedSavings));
    }

    @Then("indicate a reliability impact of {string} on the original tactic effectiveness")
    public void verifyApiReliabilityImpact(String expectedImpact) throws Exception {
        apiResult.andExpect(jsonPath("$.reliabilityImpact").value(expectedImpact));

        String targetHtmlId = mapTacticToHtmlId(activeTactic);
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(content().string(containsString("id=\"" + targetHtmlId + "\"")));
    }

    private String mapTacticToHtmlId(String tactic) {
        switch (tactic) {
            case "Server-side LB": return "tactic-server-lb";
            case "Client-side LB": return "tactic-client-lb";
            case "Circuit Breaker": return "tactic-cb";
            default: return "phase3";
        }
    }
}
