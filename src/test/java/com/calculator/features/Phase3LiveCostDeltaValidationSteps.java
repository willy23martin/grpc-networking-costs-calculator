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
public class Phase3LiveCostDeltaValidationSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions response;

    // ── Delta panel scenario ──────────────────────────────────────────────────

    @Given("the architect has set a base RPS and selected one or more tactics")
    public void setBaseRpsAndTactics() throws Exception {
        mockMvc.perform(post("/api/session/tactics")
                .param("tactic", "retry")
                .param("baseRps", "1000")
                .param("errorRate", "5%"))
                .andExpect(status().isOk());
    }

    @When("any tactic is toggled or a cloud infra value is changed")
    public void toggleTacticOrChangeCloudValue() throws Exception {
        response = mockMvc.perform(get("/api/live-delta"));
    }

    @Then("the panel immediately updates with the base cost in USD per month")
    public void verifyBaseCostInPanel() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.baseCostUsdMonth").exists());
    }

    @And("the panel shows tactics cost including networking plus cloud infra minus FinOps savings")
    public void verifyTacticsCostInPanel() throws Exception {
        response.andExpect(jsonPath("$.tacticsCostUsdMonth").exists());
    }

    @And("the panel shows a per-tactic breakdown table with each tactic type and estimated monthly impact")
    public void verifyPerTacticBreakdownTable() throws Exception {
        response.andExpect(jsonPath("$.tacticBreakdown").isArray());

        // Also verify the breakdown table element is present in calculator.html
        mockMvc.perform(get("/calculator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"live-delta-panel\"")));
    }

    @And("the panel shows a delta row in red for cost increase or green for saving")
    public void verifyDeltaRowColorCoding() throws Exception {
        response.andExpect(jsonPath("$.deltaDirection").exists());
        // deltaDirection should be either "increase" or "saving"
    }

    // ── Placeholder sizes scenario ────────────────────────────────────────────

    @Given("no proto file has been uploaded yet")
    public void noProtoFileUploaded() {
        // No action needed — default calculator state has no proto loaded.
        // Steps that need a clean state should be isolated with @Before hooks if needed.
    }

    @When("the live comparison renders")
    public void renderLiveComparison() throws Exception {
        response = mockMvc.perform(get("/api/live-delta"));
    }

    @Then("the tool uses placeholder sizes of {int} bytes request and {int} bytes response with a disclaimer")
    public void verifyPlaceholderSizes(Integer requestBytes, Integer responseBytes) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.requestBytes").value(requestBytes))
                .andExpect(jsonPath("$.responseBytes").value(responseBytes))
                .andExpect(jsonPath("$.usingPlaceholders").value(true))
                .andExpect(jsonPath("$.disclaimer").exists());
    }

    @And("once the proto is submitted in Phase 2 exact backend-calculated sizes replace the placeholders")
    public void verifyProtoSubmissionReplacesPlaceholders() throws Exception {
        // Simulate proto submission and verify placeholders are no longer used
        mockMvc.perform(post("/calculateTCO")
                        .param("bucId", "BUC1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usingPlaceholders").value(false));
    }
}
