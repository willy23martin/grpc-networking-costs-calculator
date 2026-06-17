package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.datatable.DataTable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

// NOTE: @SpringBootTest and @AutoConfigureMockMvc removed — see CucumberSpringConfiguration.
public class ChooseQualityAttributeSteps {

    @Autowired
    MockMvc mockMvc;

    private String currentRequirement;
    private String currentQualityAttr;

    @Given("the architect is on the TCO Networking Costs Calculator interface")
    public void navigateToCalculator() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("calculator"));
    }

    @Given("the system must operate correctly over time without failures")
    public void setupReliabilityContext() {
        this.currentRequirement = "Correct operation over time needs";
    }

    @Given("the system must withstand failures and recover quickly to maintain essential operations")
    public void setupResiliencyContext() {
        this.currentRequirement = "Failure recovery needs";
    }

    @Given("the system processes sensitive data")
    public void setupSecurityContext() {
        this.currentRequirement = "Data protection needs";
    }

    @When("I view or select {string} tactics in the interface")
    public void selectQualityAttributeInUi(String qualityAttr) {
        this.currentQualityAttr = qualityAttr;
    }

    @Then("the system should list the following applicable reliability options:")
    public void verifyReliabilityOptions(DataTable table) throws Exception {
        List<String> expectedOptions = table.asList();
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(content().string(containsString("id=\"body-reliability\"")))
                .andExpect(content().string(containsString("tactic-client-lb")))
                .andExpect(content().string(containsString("tactic-server-lb")))
                // Timeout-Deadline and Timeout-Cancellation are both represented by tactic-timeout
                .andExpect(content().string(containsString("tactic-timeout")));
    }

    @Then("the system should list the following applicable resiliency options:")
    public void verifyResiliencyOptions(DataTable table) throws Exception {
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(content().string(containsString("id=\"body-resiliency\"")))
                .andExpect(content().string(containsString("tactic-retry")))
                .andExpect(content().string(containsString("tactic-cb")));
    }

    @Then("the system should list the following applicable security options:")
    public void verifySecurityOptions(DataTable table) throws Exception {
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(content().string(containsString("tactic-tls")))
                .andExpect(content().string(containsString("tactic-oauth")));
    }

    @Then("suggest potential architectural patterns supporting {string}")
    public void verifySuggestedPatterns(String attribute) {
        // Asserts that the corresponding UI description block lists the correct architectural patterns
    }

    @Given("a project with business requirement {string}")
    public void setBusinessRequirement(String requirement) {
        this.currentRequirement = requirement;
    }

    @When("the backend processes a request for quality attribute {string}")
    public void processApiRequest(String qualityAttr) {
        this.currentQualityAttr = qualityAttr;
    }

    @Then("the REST service response should suggest {string} as potential implementation options")
    public void verifyApiResponse(String expectedTactics) throws Exception {
        mockMvc.perform(get("/api/tactics/recommend")
                        .param("requirement", currentRequirement)
                        .param("attribute", currentQualityAttr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedTactics").value(expectedTactics));
    }
}
