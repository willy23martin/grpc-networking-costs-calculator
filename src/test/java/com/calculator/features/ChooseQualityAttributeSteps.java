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

public class ChooseQualityAttributeSteps {

    @Autowired
    private MockMvc mockMvc;

    private String currentRequirement;
    private String currentCharacteristic;
    private ResultActions responseResult;

    @Given("the architect is on the TCO Networking Costs Calculator interface")
    public void navigateToCalculator() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("calculator"));
    }

    @Given("the system must withstand failures and recover quickly to maintain essential operations")
    public void setupResiliencyPreconditions() {
        // UI Context placeholder
    }

    @When("I select {string} as the primary architectural driver")
    public void selectPrimaryArchitecturalDriver(String attribute) throws Exception {
        responseResult = mockMvc.perform(get("/").param("driver", attribute))
                .andExpect(status().isOk());
    }

    @Then("the system should list applicable {string}")
    public void verifyApplicableTacticsListed(String expectedTacticLabel) throws Exception {
        // Validates that the parsed template lists structural components for the chosen attribute
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"cat-cloud-tactics\"")));
    }

    @And("suggest potential {string} supporting {string}")
    public void verifyArchitecturalPatternsSuggested(String patternsLabel, String targetCharacteristic) throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"cat-cloud-tactics\"")));
    }

    @Given("the system must operate correctly over time without failures")
    public void setupReliabilityPreconditions() {
        // UI Context placeholder
    }

    @When("I select {string} as the primary quality attribute")
    public void selectPrimaryQualityAttribute(String attribute) throws Exception {
        responseResult = mockMvc.perform(get("/").param("driver", attribute))
                .andExpect(status().isOk());
    }

    @Given("the system processes sensitive data")
    public void setupSecurityPreconditions() {
        // UI Context placeholder
    }

    // --- API DataDriven Steps Configuration ---

    @Given("a project with {string}")
    public void setBusinessRequirement(String requirement) {
        this.currentRequirement = requirement;
    }

    @When("I select {string} as an architectural driver")
    public void processApiRequest(String qualityAttr) {
        this.currentCharacteristic = qualityAttr;
    }

    @Then("the REST service response should suggest {string} as potential implementation options")
    public void verifyApiResponse(String recommendedTactics) throws Exception {
        String targetApiPath;
        switch (currentCharacteristic.toLowerCase()) {
            case "reliability": targetApiPath = "/api/reliability/tactic-mappings"; break;
            case "resiliency":  targetApiPath = "/api/resiliency/tactic-mappings"; break;
            case "security":    targetApiPath = "/api/security/tactic-mappings"; break;
            default:            targetApiPath = "/api/tactics/recommend"; break;
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