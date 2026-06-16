package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

// NOTE: @SpringBootTest and @AutoConfigureMockMvc removed — see CucumberSpringConfiguration.
public class CloudServiceConfigurationSteps {

    @Autowired
    private MockMvc mockMvc;

    private String targetService;
    private String activeStrategy;
    private String selectedPattern;
    private String currentTactic;
    private ResultActions responseResult;

    @Given("the architect is managing configurations on the TCO Calculator")
    public void verifyDashboardContext() throws Exception {
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(status().isOk());
    }

    // FIX: Original check tested for "AWS ACM" (present in calculator.html as a class="acm-note"
    // label), "Azure Key Vault" (absent — correct), and "Google Certificate Authority Service"
    // (absent — correct). All three checks are valid against the actual HTML content.
    @Given("the configuration dashboard is verified to contain only AWS cloud environments")
    public void verifyAwsOnlyConstraint() throws Exception {
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(content().string(containsString("AWS ACM")))
                .andExpect(content().string(not(containsString("Azure Key Vault"))))
                .andExpect(content().string(not(containsString("Google Certificate Authority Service"))));
    }

    @Given("I have selected {string} strategy for {string} service")
    public void setStrategyForService(String strategy, String service) {
        this.activeStrategy = strategy;
        this.targetService = service;
    }

    @When("I modify the service configuration with {string} option")
    public void modifyServiceConfiguration(String option) throws Exception {
        responseResult = mockMvc.perform(post("/api/finops/config/modify-commitment")
                .param("service", targetService)
                .param("strategy", activeStrategy)
                .param("commitment", option));
    }

    @Then("the system should update the ServiceFinOpsConfiguration with commitment type")
    public void verifyFinOpsConfigurationCommitment() throws Exception {
        responseResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(true))
                .andExpect(jsonPath("$.configurationType").value("COMMITMENT"));
    }

    @Then("generate implementation instructions for the operations team")
    public void verifyOperationsInstructions() throws Exception {
        responseResult.andExpect(jsonPath("$.instructions").exists());
    }

    @Given("I have selected {string} pattern with {string}")
    public void selectPatternWithTimeout(String pattern, String subPattern) {
        this.selectedPattern = pattern;
    }

    @When("I configure timeout values of {string} and failure threshold of {string}")
    public void configureResiliencyThresholds(String timeoutVal, String threshold) throws Exception {
        String rawTimeout = timeoutVal.replace("ms", "");
        responseResult = mockMvc.perform(post("/api/finops/config/resiliency")
                .param("timeoutMs", rawTimeout)
                .param("failureThreshold", threshold));
    }

    @Then("the system should update the ServiceFinOpsConfiguration with these parameters")
    public void verifyResiliencyConfigurationParameters() throws Exception {
        responseResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.timeoutMs").value(2000))
                .andExpect(jsonPath("$.failureThreshold").value(5));
    }

    @Then("generate implementation code examples for the gRPC services")
    public void verifyGrpcCodeGeneration() throws Exception {
        responseResult.andExpect(jsonPath("$.grpcStubExample").exists());
    }

    @Given("I have implemented {string} using {string}")
    public void setupSecurityTacticContext(String securityTactic, String cloudService) {
        this.currentTactic = securityTactic;
        this.targetService = cloudService;
    }

    @When("I modify the configuration with {string}")
    public void applySecurityOptimization(String parameter) throws Exception {
        responseResult = mockMvc.perform(post("/api/finops/config/security-optimize")
                .param("tactic", currentTactic)
                .param("service", targetService)
                .param("parameter", parameter));
    }

    @Then("the system should update the ServiceFinOpsConfiguration")
    public void verifyGenericFinOpsConfigurationUpdate() throws Exception {
        responseResult.andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Then("estimate {string} while maintaining {string}")
    public void verifySavingsAndSecurityLevel(String expectedSavings, String expectedSecurityLevel) throws Exception {
        responseResult.andExpect(jsonPath("$.monthlySavings").value(expectedSavings))
                .andExpect(jsonPath("$.securityLevel").value(expectedSecurityLevel));
    }
}
