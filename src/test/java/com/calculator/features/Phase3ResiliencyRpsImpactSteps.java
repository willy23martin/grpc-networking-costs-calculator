package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

// NOTE: @SpringBootTest / @AutoConfigureMockMvc absent — see CucumberSpringConfiguration.
public class Phase3ResiliencyRpsImpactSteps {

    @Autowired
    private MockMvc mockMvc;

    private int baseRps;
    private String errorRate;
    private ResultActions response;

    @Given("the base RPS is {int} req/s")
    public void setBaseRpsReqPerSec(Integer rps) {
        this.baseRps = rps;
    }

    @Given("any base RPS")
    public void setAnyBaseRps() {
        this.baseRps = 1000;
    }

    // FIX: All POST /api/session/tactics → GET /api/session/tactics
    @When("I enable the Retry tactic with a {int}% error rate")
    public void enableRetryTactic(Integer errorRatePercent) throws Exception {
        this.errorRate = errorRatePercent + "%";
        response = mockMvc.perform(get("/api/session/tactics")
                .param("tactic", "retry")
                .param("baseRps", String.valueOf(baseRps))
                .param("errorRate", errorRate));
    }

    @When("I enable the Circuit Breaker tactic")
    public void enableCircuitBreakerTactic() throws Exception {
        response = mockMvc.perform(get("/api/session/tactics")
                .param("tactic", "circuit-breaker")
                .param("baseRps", String.valueOf(baseRps)));
    }

    @Then("the effective RPS increases by {int} req/s to {int} req/s")
    public void verifyEffectiveRpsIncrease(Integer delta, Integer expectedRps) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveRps").value(expectedRps))
                .andExpect(jsonPath("$.rpsDelta").value(delta));
    }

    @Then("the effective RPS is unchanged")
    public void verifyEffectiveRpsUnchanged() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveRps").value(baseRps))
                .andExpect(jsonPath("$.rpsDelta").value(0));
    }

    @And("the live cost delta panel shows the additional egress cost")
    public void verifyLiveCostDeltaPanelPresent() throws Exception {
        mockMvc.perform(get("/calculator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"live-delta-panel\"")));
    }

    @And("the tactic appears in the {string} row of the tactics breakdown")
    public void verifyTacticRowInBreakdown(String rowType) throws Exception {
        response.andExpect(jsonPath("$.tacticCategory").value(rowType));
    }

    @When("Retry is enabled with {string}")
    public void enableRetryWithErrorRate(String errorRateStr) throws Exception {
        this.errorRate = errorRateStr;
        response = mockMvc.perform(get("/api/session/tactics")
                .param("tactic", "retry")
                .param("baseRps", String.valueOf(baseRps))
                .param("errorRate", errorRateStr));
    }

    @Then("effective RPS becomes {int}")
    public void verifyEffectiveRps(Integer expectedRps) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveRps").value(expectedRps));
    }

    @And("monthly egress cost increase is approximately {string}")
    public void verifyMonthlyCostIncrease(String expectedCostDelta) throws Exception {
        response.andExpect(jsonPath("$.monthlyCostDelta").value(expectedCostDelta));
    }
}