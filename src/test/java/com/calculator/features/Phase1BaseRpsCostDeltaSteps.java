package com.calculator.features;

import com.calculator.infrastructure.cloud.adapters.aws.networking.AWSDataTransferCostCalculationServiceAdapter;
import com.calculator.domain.dto.requests.EffectiveRequestPerSecondRequest;
import com.calculator.domain.dto.responses.EffectiveRequestPerSecondResponse;
import com.calculator.infrastructure.web.rest.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static com.calculator.application.services.calculators.CostEfficiencyCalculator.SECONDS_PER_MONTH;
import static com.calculator.infrastructure.web.rest.NetworkingCostCalculatorController.BYTES_PER_GB;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class Phase1BaseRpsCostDeltaSteps extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AWSDataTransferCostCalculationServiceAdapter dataTransferAdapter;

    private int baseRps;
    private EffectiveRequestPerSecondRequest requestPayload;
    private EffectiveRequestPerSecondResponse apiResponse;

    @Given("a service with {int} requests per second")
    public void setBaseRps(Integer rps) {
        this.baseRps = rps;
        this.requestPayload = new EffectiveRequestPerSecondRequest();
        this.requestPayload.setBaseRequestPerSecond(rps);
    }

    @When("the architect selects a tactic that increases effective RPS by {string}")
    public void selectTacticWithRpsDelta(String rpsDelta) throws Exception {
        if (rpsDelta.contains("Retry Profile")) {
            this.requestPayload.setRetryEnabled(true);
            this.requestPayload.setRetryErrorPercentage(5);
        } else if (rpsDelta.contains("SAGA Cascading")) {
            this.requestPayload.setRetryEnabled(true);
            this.requestPayload.setRetryErrorPercentage(100);
        }

        MvcResult result = mockMvc.perform(post("/api/tco/effective-rps")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestPayload)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        this.apiResponse = objectMapper.readValue(responseBody, EffectiveRequestPerSecondResponse.class);
        assertNotNull(this.apiResponse, "The API response from /api/tco/effective-rps was null.");
    }

    @Then("the live cost delta panel shows an egress cost increase of approximately {string} per month")
    public void verifyEgressCostDelta(String expectedDeltaUsd) {
        double expectedCost = Double.parseDouble(
                expectedDeltaUsd.replace("$", "")
                        .replace("/ mo", "")
                        .trim()
        );

        double effectiveRps = apiResponse.getEffectiveRps();
        double baseRpsFromApi = apiResponse.getBaseRps();
        double rpsDelta = Math.abs(effectiveRps - baseRpsFromApi);

        double responseSizeEffectiveBytes = 1200.0;
        double secondsInMonth = SECONDS_PER_MONTH;
        double bytesInGb = BYTES_PER_GB;

        double calculatedGbPerMonth = (responseSizeEffectiveBytes * rpsDelta * secondsInMonth) / bytesInGb;

        assertNotNull(dataTransferAdapter, "Data transfer cost adapter must be wired.");
        double calculatedCost = dataTransferAdapter.calculateDataTransferCost(calculatedGbPerMonth);

        assertEquals(expectedCost, calculatedCost, 0.05,
                String.format("Egress incremental cost calculation mismatch. Expected: $%s, Calculated: $%s",
                        expectedCost, calculatedCost));
    }
}