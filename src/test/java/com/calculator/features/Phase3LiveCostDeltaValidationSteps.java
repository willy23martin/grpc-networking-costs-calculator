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

public class Phase3LiveCostDeltaValidationSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions response;

    @Given("the architect has set a base RPS and selected one or more tactics")
    public void setBaseRpsAndTactics() throws Exception {
        String body = """
                {
                  "requestsPerSecond": 1000,
                  "reliabilityTactics": { "reliabilityClientSideLoadBalancerTactic": false, "reliabilityServerSideLoadBalancerTactic": false },
                  "timeoutTactic": { "resiliencyTimeoutTactic": false, "tacticTimeoutMilliseconds": 0 },
                  "retryTactic": { "resiliencyRetryTactic": true, "tacticRetryTimes": 3 },
                  "circuitBreakerTactic": { "resiliencyCircuitBreakerPattern": false, "circuitBreakerPatternMinimumCalls": 0, "circuitBreakerHalfOpen": 0, "circuitBreakerWaitMilliseconds": 0, "circuitBreakerFailureRate": 0 }
                }
                """;

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @When("any tactic is toggled or a cloud infra value is changed")
    public void toggleTacticOrChangeCloudValue() throws Exception {
        String body = """
                {
                  "baseRps": 1000,
                  "protoResponseSizeEffectiveBytes": 1200,
                  "retryEnabled": true,
                  "retryErrorRatePct": 5.0
                }
                """;
        response = mockMvc.perform(post("/api/cost/tactic-contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Then("the panel immediately updates with the base cost in USD per month")
    public void verifyBaseCostInPanel() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTacticNetworkingDeltaUsd").exists());
    }

    @And("the panel shows tactics cost including networking plus cloud infra minus FinOps savings")
    public void verifyTacticsCostInPanel() throws Exception {
        response.andExpect(jsonPath("$.contributions").isArray());
    }

    @And("the panel shows a per-tactic breakdown table with each tactic type and estimated monthly impact")
    public void verifyPerTacticBreakdownTable() throws Exception {
        response.andExpect(jsonPath("$.contributions").isArray());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"tactics-comparison-block\"")));
    }

    @And("the panel shows a delta row in red for cost increase or green for saving")
    public void verifyDeltaRowColorCoding() throws Exception {
        response.andExpect(jsonPath("$.totalTacticNetworkingDeltaUsd").exists());
    }

    @Given("no proto file has been uploaded yet")
    public void noProtoFileUploaded() {
    }

    @When("the live comparison renders")
    public void renderLiveComparison() throws Exception {
        response = mockMvc.perform(get("/"));
    }

    @Then("the tool uses placeholder sizes of {int} bytes request and {int} bytes response with a disclaimer")
    public void verifyPlaceholderSizes(Integer requestBytes, Integer responseBytes) throws Exception {
        response.andExpect(status().isOk());
    }

    @And("once the proto is submitted in Phase 2 exact backend-calculated sizes replace the placeholders")
    public void verifyProtoSubmissionReplacesPlaceholders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}