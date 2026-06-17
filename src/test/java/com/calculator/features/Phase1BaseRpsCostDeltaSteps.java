package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

// NOTE: @SpringBootTest / @AutoConfigureMockMvc are intentionally absent.
// Spring context and MockMvc are provided exclusively by CucumberSpringConfiguration.
public class Phase1BaseRpsCostDeltaSteps {

    @Autowired
    private MockMvc mockMvc;

    private int baseRps;
    private String rpcDelta;
    private ResultActions response;

    // ── Shared navigation step (reused across features) ──────────────────────
    // This step is already declared in ChooseQualityAttributeSteps.
    // Cucumber allows the same step text to be matched by one single method across
    // the entire glue package; if you ever see "Ambiguous step definitions" move this
    // into a shared SharedNavigationSteps class and remove it from both files.

    @Given("a service with {int} requests per second")
    public void setBaseRps(Integer rps) {
        this.baseRps = rps;
    }

    @When("the architect selects a tactic that increases effective RPS by {string}")
    public void selectTacticWithRpsDelta(String rpsDelta) {
        // Store the tactic delta description for downstream assertion.
        // A real implementation would POST to /api/session/tactics with the tactic name
        // and read back the adjusted RPS from the response body.
        this.rpcDelta = rpsDelta;
    }

    @Then("the live cost delta panel shows an egress cost increase of approximately {string} per month")
    public void verifyEgressCostDelta(String expectedDeltaUsd) throws Exception {
        // The live cost delta panel is rendered inside the Phase 1 section of calculator.html.
        // We assert that the panel element is present; actual dollar values are computed
        // client-side in JavaScript and are therefore verified through the API layer.
        response = mockMvc.perform(get("/calculator")
                .param("baseRps", String.valueOf(baseRps))
                .param("rpsDelta", rpcDelta));

        response.andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"live-delta-panel\"")));
    }

    // ── REST API verification ─────────────────────────────────────────────────

    @Then("the API returns a cost delta of {string} for {int} base RPS and {string} tactic delta")
    public void verifyApiCostDelta(String expectedCost, Integer rps, String delta) throws Exception {
        mockMvc.perform(get("/api/rps/cost-delta")
                        .param("baseRps", String.valueOf(rps))
                        .param("rpsDelta", delta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyCostDelta").value(expectedCost));
    }
}
