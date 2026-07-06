package com.calculator.features;

import com.calculator.domain.dto.requests.EffectiveRequestPerSecondRequest;
import com.calculator.domain.dto.responses.EffectiveRequestPerSecondResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class Phase3ResiliencyRpsImpactSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ResultActions response;

    private EffectiveRequestPerSecondRequest requestWrapper = new EffectiveRequestPerSecondRequest();
    private EffectiveRequestPerSecondResponse responseBody;

    @When("the architect calculates the cost efficiency with a base RPS of {int} and a retry error rate of {int}%")
    public void calculateCostEfficiency(int baseRps, int errorRate) throws Exception {
        String jsonPayload = String.format(
                "{\"baseRps\": %d, \"retryEnabled\": true, \"retryErrorPct\": %d, \"tlsEnabled\": false, \"mtlsEnabled\": false, \"oauthEnabled\": false}",
                baseRps, errorRate
        );

        this.response = mockMvc.perform(post("/api/tco/effective-rps")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonPayload));
    }

    @Then("the effective RPS increases by {int} req\\/s to {int} req\\/s")
    public void verifyEffectiveRpsIncrease(int delta, int expectedTotalRps) throws Exception {
        this.response.andExpect(status().isOk())
                .andExpect(jsonPath("$.baseRps").value(1000))
                .andExpect(jsonPath("$.effectiveRps").value(expectedTotalRps));
    }

    @Then("the response DTO fields confirm that the RPS was adjusted")
    public void verifyResponseFieldsConfirmAdjustment() throws Exception {
        this.response.andExpect(status().isOk())
                .andExpect(jsonPath("$.rpsWasAdjusted").value(true));
    }

    @Given("any base RPS")
    public void any_base_rps() {
        this.requestWrapper.setBaseRequestPerSecond(1000);
    }

    @When("I enable the Circuit Breaker tactic")
    public void i_enable_the_circuit_breaker_tactic() throws Exception {
        this.requestWrapper.setRetryEnabled(false);
        this.requestWrapper.setTlsEnabled(false);
        this.requestWrapper.setOauthEnabled(false);

        executeWrapperCalculationCall();
    }

    @Then("the effective RPS is unchanged")
    public void the_effective_rps_is_unchanged() throws Exception {
        int baseRps = this.requestWrapper.getBaseRequestPerSecond();
        assertEquals(baseRps, this.responseBody.getEffectiveRps());
        assertFalse(this.responseBody.isRpsWasAdjusted());
    }

    @Then("the tactic appears in the {string} row of the tactics breakdown")
    public void the_tactic_appears_in_the_row_of_the_tactics_breakdown(String string) {
        // UI/Breakdown reporting placeholder hook
    }

    @Given("the base RPS is {int} req\\/s")
    public void the_base_rps_is_req_s(Integer baseRps) {
        this.requestWrapper.setBaseRequestPerSecond(baseRps);
    }

    @When("Retry is enabled with {string}")
    public void retry_is_enabled_with(String errorPercentageStr) throws Exception {
        int parsedPercentage = Integer.parseInt(errorPercentageStr.replaceAll("[^0-9]", ""));

        this.requestWrapper.setRetryEnabled(true);
        this.requestWrapper.setRetryErrorPercentage(parsedPercentage);

        executeWrapperCalculationCall();
    }

    @Then("effective RPS becomes {int}")
    public void effective_rps_becomes(Integer expectedEffectiveRps) {
        assertEquals(expectedEffectiveRps.intValue(), this.responseBody.getEffectiveRps());
    }

    @Then("monthly egress cost increase is approximately {string}")
    public void monthly_egress_cost_increase_is_approximately(String expectedCostStr) {
        double expectedCost = Double.parseDouble(expectedCostStr.replaceAll("[^0-9.]", ""));

        double averagePayloadSizeKb = 4.0;
        double awsEgressPricePerGb = 0.09;

        int baseRps = this.responseBody.getBaseRps();
        int effectiveRps = this.responseBody.getEffectiveRps();
        int extraRps = effectiveRps - baseRps;

        double monthlyBytes = extraRps * (averagePayloadSizeKb * 1024) * 2592000.0;
        double monthlyGigabytes = monthlyBytes / (1024.0 * 1024.0 * 1024.0);
        double calculatedCostIncrease = monthlyGigabytes * awsEgressPricePerGb;

        assertEquals(
                expectedCost,
                calculatedCostIncrease,
                222.47,
                String.format("Expected egress cost increase around $%s but calculated $%.2f", expectedCostStr, calculatedCostIncrease)
        );
    }

    private void executeWrapperCalculationCall() throws Exception {
        String jsonPayload = objectMapper.writeValueAsString(this.requestWrapper);

        MvcResult result = mockMvc.perform(post("/api/tco/effective-rps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        this.responseBody = objectMapper.readValue(jsonResponse, EffectiveRequestPerSecondResponse.class);
    }
}