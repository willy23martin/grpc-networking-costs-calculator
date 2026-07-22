package com.calculator.features;

import com.calculator.domain.dto.tactics.reliability.ReliabilityTradeoffDTO;
import com.calculator.infrastructure.web.rest.BaseIntegrationTest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class Activity4BaseCostCalculationSteps extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String selectedCloudService;
    private String selectedTactic;
    private String cloudProvider;
    private String usageParameter;

    private List<ReliabilityTradeoffDTO> responseMappings;

    @Given("I have selected {string} to implement {string}")
    public void i_have_selected_to_implement(String cloudService, String tactic) {
        this.selectedCloudService = cloudService;
        this.selectedTactic = tactic;
    }

    @Given("my base cost provider is {string}")
    public void my_base_cost_provider_is(String provider) {
        this.cloudProvider = provider;
    }

    @When("I trigger the base cost calculation")
    public void i_trigger_the_base_cost_calculation() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reliability/tactic-mappings")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        this.responseMappings = objectMapper.readValue(jsonResponse, new TypeReference<List<ReliabilityTradeoffDTO>>() {});
    }

    @Then("the system should show {string} estimates")
    public void the_system_should_show_estimates(String hourlyCost) {
        assertNotNull(responseMappings, "The API response should not be null");

        ReliabilityTradeoffDTO matchedService = responseMappings.stream()
                .filter(dto -> dto.getTacticName().equalsIgnoreCase(selectedCloudService) &&
                        dto.getCloudProvider().equalsIgnoreCase(cloudProvider))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cloud service " + selectedCloudService + " for provider " + cloudProvider + " not found."));

        String actualCostFactorNotes = matchedService.getCostFactor();
        assertNotNull(actualCostFactorNotes, "Cost factor notes property should not be null");

        assertTrue(actualCostFactorNotes.contains(hourlyCost),
                String.format("Expected cost factor notes to state the hourly rate '%s'. Actual content: \n[%s]",
                        hourlyCost, actualCostFactorNotes));
    }

    @Then("highlight {string} as primary cost drivers")
    public void highlight_as_primary_cost_drivers(String expectedCostFactors) {
        ReliabilityTradeoffDTO matchedService = responseMappings.stream()
                .filter(dto -> dto.getTacticName().equalsIgnoreCase(selectedCloudService))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Cloud service " + selectedCloudService + " not found in response mappings."));

        String actualCostFactorNotes = matchedService.getCostFactor();
        assertNotNull(actualCostFactorNotes, "Cost factor notes property should not be null");

        assertTrue(actualCostFactorNotes.contains(expectedCostFactors),
                String.format("Expected cost factor string to contain: \n[%s]\nBut actual notes were: \n[%s]",
                        expectedCostFactors, actualCostFactorNotes));
    }
}