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

public class Activity3MapCloudServicesToImplementTheChosenTacticsSteps extends BaseIntegrationTest {

    private String architecturalCharacteristic;
    private String patternOrTactic;
    private String cloudProvider;

    @Given("the architect is authenticated on the TCO Calculator application")
    public void architectIsAuthenticated() {
    }

    @Given("the architect is configuring tactics in the Cloud Tactics & Patterns panel")
    public void architectIsConfiguringTactics() {
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
        final String targetApiPath = getEndpointForCharacteristic(architecturalCharacteristic);

        // Execute the API call and get the raw JSON response payload
        final MvcResult result = mockMvc.perform(get(targetApiPath))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        final String responseBody = result.getResponse().getContentAsString();

        final String parentTacticIdPath = "$[?(@.tacticName == '" + patternOrTactic + "')].tacticId";
        final List<String> parentIdList = JsonPath.read(responseBody, parentTacticIdPath);

        if (parentIdList.isEmpty()) {
            throw new AssertionError("Could not find a parent tactic matching the name: " + patternOrTactic);
        }
        final String parentTacticId = parentIdList.getFirst();

        final String[] expectedServices = Arrays.stream(expectedServicesString.split(","))
                .map(String::trim)
                .toArray(String[]::new);

        for (String serviceName : expectedServices) {
            final String serviceDecisionsPath = "$[?(@.tacticName == '" + serviceName + "' && @.cloudProvider == '" + cloudProvider + "')].supportedArchitecturalDecisions[*]";
            final List<String> supportedDecisions = JsonPath.read(responseBody, serviceDecisionsPath);

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