package com.calculator.features;

import com.calculator.infrastructure.web.rest.BaseIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class MapCloudServicesToImplementTheChosenTacticsSteps extends BaseIntegrationTest {

    private String architecturalCharacteristic;
    private String patternOrTactic;
    private String cloudProvider;

    @Given("the architect is authenticated on the TCO Calculator application")
    public void architectIsAuthenticated() {
        // MockMvc context authentication setup if needed
    }

    @Given("the architect is configuring tactics in the Cloud Tactics & Patterns panel")
    public void architectIsConfiguringTactics() {
        // Context placeholder matching the background step
    }

    @Given("I have selected pattern or tactic {string} for implementation that promotes {string}")
    public void selectPatternOrTacticWithCharacteristic(String patternOrTactic, String architecturalCharacteristic) {
        this.patternOrTactic = patternOrTactic;
        this.architecturalCharacteristic = architecturalCharacteristic;
    }

    @When("I specify {string} as my deployment target")
    public void specifyCloudProvider(String cloudProvider) {
        this.cloudProvider = cloudProvider;
    }

    @Then("the system should map the configuration to specific services {string}")
    public void verifyCloudServiceMapping(String expectedServicesString) throws Exception {
        String targetApiPath = getEndpointForCharacteristic(architecturalCharacteristic);

        // Execute the API call and get the raw JSON response payload
        MvcResult result = mockMvc.perform(get(targetApiPath))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();

        // 1. Extract the parent tacticId dynamically based on the patternOrTactic name
        // Example: Finds "tactic-server-lb" where tacticName matches "Server-side Load Balancing"
        String parentTacticIdPath = "$[?(@.tacticName == '" + patternOrTactic + "')].tacticId";
        List<String> parentIdList = JsonPath.read(responseBody, parentTacticIdPath);

        if (parentIdList.isEmpty()) {
            throw new AssertionError("Could not find a parent tactic matching the name: " + patternOrTactic);
        }
        String parentTacticId = parentIdList.get(0);

        // 2. Split comma-separated cloud service names from the Examples table
        String[] expectedServices = Arrays.stream(expectedServicesString.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        // 3. For each expected cloud service, assert it links back to the parent tactic ID
        for (String serviceName : expectedServices) {
            String serviceDecisionsPath = "$[?(@.tacticName == '" + serviceName + "' && @.cloudProvider == '" + cloudProvider + "')].supportedArchitecturalDecisions[*]";
            List<String> supportedDecisions = JsonPath.read(responseBody, serviceDecisionsPath);

            assertTrue(
                    supportedDecisions.contains(parentTacticId),
                    String.format("Expected cloud service '%s' to support parent tactic ID '%s' (%s), but its decisions were: %s",
                            serviceName, parentTacticId, patternOrTactic, supportedDecisions)
            );
        }
    }

    private String getEndpointForCharacteristic(String characteristic) {
        return switch (characteristic.toLowerCase()) {
            case "security" -> "/api/security/tactic-mappings";
            case "reliability" -> "/api/reliability/tactic-mappings";
            case "resiliency" -> "/api/resiliency/tactic-mappings";
            default -> throw new IllegalArgumentException("Unknown characteristic: " + characteristic);
        };
    }
}