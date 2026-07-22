package com.calculator.features;

import com.calculator.infrastructure.web.rest.BaseIntegrationTest;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class Activity1ChooseQualityAttributeSteps extends BaseIntegrationTest {

    private String currentRequirement;
    private String currentCharacteristic;

    @Given("the architect is on the TCO Networking Costs Calculator interface")
    public void navigateToCalculator() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("calculator"));
    }

    @Given("a project with {string}")
    public void setBusinessRequirement(String businessRequirement) {
        this.currentRequirement = businessRequirement;
    }

    @When("I select {string} as an architectural driver")
    public void processApiRequest(String architecturalDriver) {
        this.currentCharacteristic = architecturalDriver;
    }

    @Then("the system should suggest {string} as potential implementation options")
    public void verifyApiResponse(String recommendedTactics) throws Exception {
        String targetApiPath;
        switch (currentCharacteristic.toLowerCase()) {
            case "reliability": targetApiPath = "/api/reliability/tactic-mappings"; break;
            case "resiliency":  targetApiPath = "/api/resiliency/tactic-mappings"; break;
            case "security":    targetApiPath = "/api/security/tactic-mappings"; break;
            default:            targetApiPath = "/api/reliability/tactic-mappings"; break;
        }

        String responseContent = mockMvc.perform(get(targetApiPath)
                        .param("requirement", currentRequirement))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        verify(recommendedTactics, responseContent);
    }

    private static void verify(String recommendedTactics, String responseContent) {
        String[] tacticsArray = recommendedTactics.split(",");
        for (String tactic : tacticsArray) {
            String trimmedTactic = tactic.trim();
            org.hamcrest.MatcherAssert.assertThat(
                    responseContent,
                    org.hamcrest.Matchers.containsString(trimmedTactic)
            );
        }
    }
}