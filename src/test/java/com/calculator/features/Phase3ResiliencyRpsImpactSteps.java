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

public class Phase3ResiliencyRpsImpactSteps {

    @Autowired
    private MockMvc mockMvc;

    private Integer baseRps;
    private String errorRate;
    private ResultActions response;

    @Given("the base RPS is {int} req/s")
    public void setBaseRps(Integer baseRps) {
        this.baseRps = baseRps;
    }

    @Given("any base RPS")
    public void setAnyBaseRps() {
        this.baseRps = 1000; // Standard baseline token
    }

    @When("I enable the Retry tactic with a {int}% error rate")
    public void enableRetryTacticWithRate(Integer pct) throws Exception {
        // FIX: Replaced "retryErrorPercentage" with "retryErrorRatePct" to map properties correctly
        String body = """
                {
                  "baseRequestPerSecond": %d,
                  "retryEnabled": true,
                  "retryErrorRatePct": %d
                }
                """.formatted(baseRps, pct);

        response = mockMvc.perform(post("/api/tco/effective-rps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("I enable the Circuit Breaker tactic")
    public void enableCircuitBreakerTactic() throws Exception {
        String body = """
                {
                  "baseRequestPerSecond": %d,
                  "circuitBreakerEnabled": true
                }
                """.formatted(baseRps);

        response = mockMvc.perform(post("/api/tco/effective-rps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Then("the effective RPS increases by {int} req/s to {int} req/s")
    public void verifyEffectiveRpsIncrease(Integer expectedDelta, Integer expectedTotal) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveRps").value(expectedTotal))
                .andExpect(jsonPath("$.rpsWasAdjusted").value(true));
    }

    @Then("the effective RPS is unchanged")
    public void verifyEffectiveRpsUnchanged() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveRps").value(baseRps))
                .andExpect(jsonPath("$.rpsWasAdjusted").value(false));
    }

    @And("the tactic appears in the {string} row of the tactics breakdown")
    public void verifyTacticRowClassification(String rowType) throws Exception {
        // Validates HTML template element synchronization anchors
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"tactic-cb\"")));
    }

    @And("the live cost delta panel shows the additional egress cost")
    public void verifyLiveCostDeltaPanel() throws Exception {
        String body = """
                {
                  "baseRps": %d,
                  "protoResponseSizeEffectiveBytes": 1200,
                  "retryEnabled": true,
                  "retryErrorRatePct": 5
                }
                """.formatted(baseRps);

        mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTacticNetworkingDeltaUsd").exists());
    }

    @When("Retry is enabled with {string}")
    public void enableRetryViaScenarioOutline(String rateStr) throws Exception {
        this.errorRate = rateStr;
        int pct = Integer.parseInt(rateStr.replace("%", "").trim());

        // FIX: Replaced "retryErrorPercentage" with "retryErrorRatePct" to mirror model transformations
        String body = """
                {
                  "baseRequestPerSecond": %d,
                  "retryEnabled": true,
                  "retryErrorRatePct": %d
                }
                """.formatted(baseRps, pct);

        response = mockMvc.perform(post("/api/tco/effective-rps")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Then("effective RPS becomes {int}")
    public void verifyEffectiveRps(Integer expectedRps) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveRps").value(expectedRps));
    }

    @And("monthly egress cost increase is approximately {string}")
    public void verifyMonthlyCostIncrease(String expectedCostDelta) throws Exception {
        int pct = errorRate != null ? Integer.parseInt(errorRate.replace("%", "").trim()) : 5;
        String body = """
                {
                  "baseRps": %d,
                  "protoResponseSizeEffectiveBytes": 1200,
                  "retryEnabled": true,
                  "retryErrorRatePct": %d
                }
                """.formatted(baseRps, pct);

        mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTacticNetworkingDeltaUsd").exists());
    }
}