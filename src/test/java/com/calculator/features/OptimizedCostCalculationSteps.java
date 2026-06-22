package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class OptimizedCostCalculationSteps {

    @Autowired
    private MockMvc mockMvc;

    private String targetComponent;
    private String appliedStrategy;
    private String currentPattern;
    private ResultActions responseActions;

    /**
     * Helper method to synthesize a minimal, valid Protocol Buffers multipart payload
     * to satisfy the TCO Calculator Controller's parameter requirements.
     */
    private MockMultipartFile createMockProtoFile() {
        return new MockMultipartFile(
                "file",
                "architecture.proto",
                "text/plain",
                "syntax = \"proto3\";".getBytes()
        );
    }

    @Given("the architect is assessing optimized reports on the TCO Calculator")
    public void verifyReportContextPresence() throws Exception {
        mockMvc.perform(multipart("/calculateTCO").file(createMockProtoFile()))
                .andExpect(status().isOk());
    }

    @Given("the calculation baseline strictly targets AWS infrastructure services")
    public void verifyNoMultiCloudProviders() throws Exception {
        // Enforce exclusion of alternative vendor naming to remain aligned with calculator.html
        mockMvc.perform(multipart("/calculateTCO").file(createMockProtoFile()))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Azure"))))
                .andExpect(content().string(not(containsString("GCP"))));
    }

    @Given("I have modified {string} configuration with {string} strategy")
    public void setModifiedComponentStrategy(String component, String strategy) {
        this.targetComponent = component;
        this.appliedStrategy = strategy;
    }

    @When("I recalculate the service costs")
    public void executeRecalculation() throws Exception {
        responseActions = mockMvc.perform(post("/api/finops/costs/recalculate")
                .param("component", targetComponent)
                .param("strategy", appliedStrategy));
    }

    @Then("the system should show reduced monthly costs of {string}")
    public void verifyReducedMonthlyCosts(String expectedCost) throws Exception {
        responseActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.reducedMonthlyCost").value(expectedCost));
    }

    @Then("calculate the total savings over the commitment period as {string}")
    public void verifyTotalCommitmentSavings(String expectedSavings) throws Exception {
        responseActions.andExpect(jsonPath("$.totalSavings").value(expectedSavings));
    }

    @Given("I have modified {string} with optimized circuit breaker parameters")
    public void setOptimizedCircuitBreakerContext(String component) {
        this.targetComponent = component;
    }

    @When("I recalculate the service costs with actual usage metrics")
    public void executeRecalculationWithMetrics() throws Exception {
        responseActions = mockMvc.perform(post("/api/finops/costs/recalculate-metrics")
                .param("component", targetComponent));
    }

    @Then("the system should show reduced error rates by {string}")
    public void verifyReducedErrorRates(String expectedErrorRate) throws Exception {
        responseActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.reducedErrorRate").value(expectedErrorRate));
    }

    @Then("calculate monthly cost savings of {string} from reduced retry operations")
    public void verifyRetryMonthlySavings(String expectedSavings) throws Exception {
        responseActions.andExpect(jsonPath("$.monthlySavings").value(expectedSavings));
    }

    @Given("I have applied {string} to services implementing {string}")
    public void setupStrategyAndPatternContext(String strategy, String pattern) {
        this.appliedStrategy = strategy;
        this.currentPattern = pattern;
    }

    @When("I recalculate total costs across the architecture")
    public void executeArchitectureTotalRecalculation() throws Exception {
        responseActions = mockMvc.perform(post("/api/finops/costs/recalculate-architecture")
                .param("strategy", appliedStrategy)
                .param("pattern", currentPattern));
    }

    @Then("the system should show {string} as the optimized cost")
    public void verifyTotalOptimizedCost(String expectedCost) throws Exception {
        responseActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.optimizedMonthlyCost").value(expectedCost));
    }

    // FIX: The original step name was "calculate {string} while maintaining {string}" which
    // CONFLICTS with CloudServiceConfigurationSteps#verifySavingsAndSecurityLevel which uses
    // "estimate {string} while maintaining {string}". These are different step texts so there
    // is no ambiguity, but the JSON field names here (annualSavings / availabilityTarget) must
    // be distinct from the security step's fields (monthlySavings / securityLevel) in the backend.
    @Then("calculate {string} while maintaining {string}")
    public void verifySavingsAndAvailability(String expectedSavings, String expectedSla) throws Exception {
        responseActions.andExpect(jsonPath("$.annualSavings").value(expectedSavings))
                .andExpect(jsonPath("$.availabilityTarget").value(expectedSla));

        // Verify the SLA input field is present in the calculator UI
        mockMvc.perform(multipart("/calculateTCO").file(createMockProtoFile()))
                .andExpect(content().string(containsString("id=\"input-sla\"")));
    }
}