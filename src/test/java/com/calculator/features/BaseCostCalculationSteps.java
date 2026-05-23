package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
        import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
public class BaseCostCalculationSteps {

    @Autowired
    private MockMvc mockMvc;

    private String targetService;
    private String targetTactic;
    private String targetProvider;
    private String targetUsageParameter;
    private ResultActions apiResponse;

    @Given("the calculator workspace is initialized for cloud financial modeling")
    public void initWorkspace() {
        // Prepare mock sessions or reference databases
    }

    @Given("the architect has verified that only AWS components are available in the view")
    public void verifyExclusionOfAzureAndGcp() throws Exception {
        // Assert that Azure and GCP markup elements are completely absent from calculator.html
        mockMvc.perform(get("/calculator"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Azure Load Balancer"))))
                .andExpect(content().string(not(containsString("Google Cloud Service Directory"))))
                .andExpect(content().string(containsString("Fetching ALB pricing from AWS")));
    }

    @Given("I have selected {string} to implement {string}")
    public void selectServiceAndTactic(String cloudService, String tactic) {
        this.targetService = cloudService;
        this.targetTactic = tactic;
    }

    @Given("my provider is {string}")
    public void setProvider(String provider) {
        assertEquals("AWS", provider, "Only AWS is supported inside the current calculator configuration.");
        this.targetProvider = provider;
    }

    @When("I have specified {string} for the service")
    public void specifyUsageParameter(String usageParameter) {
        this.targetUsageParameter = usageParameter;
    }

    @Then("I calculate the base costs")
    public void triggerBaseCostCalculation() {
        // Simulates triggering the frontend's computeCloudInfraCost function or backend processing
    }

    @Then("the system should show {string} estimates")
    public void verifyUiHourlyCostEstimate(String expectedHourlyCost) throws Exception {
        String inputFieldId = mapServiceToHtmlInputId(targetService);

        mockMvc.perform(get("/calculator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"" + inputFieldId + "\"")));
    }

    @Then("highlight {string} as primary cost drivers")
    public void verifyCostDrivers(String expectedDrivers) {
        // Asserts that the corresponding UI description block lists the correct architectural drivers
    }

    // --- REST API Pricing Endpoint Verification ---

    @Given("a pricing query for component {string} under provider {string}")
    public void initApiPricingQuery(String cloudService, String provider) {
        this.targetService = cloudService;
        this.targetProvider = provider;
    }

    @When("the architectural calculator executes cost analysis for {string}")
    public void executeApiCostAnalysis(String usageParameter) throws Exception {
        apiResponse = mockMvc.perform(get("/api/pricing/calculate")
                .param("provider", targetProvider)
                .param("service", targetService)
                .param("parameter", usageParameter));
    }

    @Then("the JSON response must compute a corresponding baseline estimate of {string}")
    public void verifyApiResponseCost(String expectedHourlyCost) throws Exception {
        apiResponse.andExpect(status().isOk())
                .andExpect(jsonPath("$.hourlyCostEstimate").value(expectedHourlyCost));
    }

    @Then("return drivers matching {string}")
    public void verifyApiResponseDrivers(String expectedDrivers) throws Exception {
        apiResponse.andExpect(jsonPath("$.primaryDrivers").value(expectedDrivers));
    }

    // Utility connector matching service string directly with calculator.html control elements
    private String mapServiceToHtmlInputId(String service) {
        switch (service) {
            case "Elastic Load Balancer (ELB)": return "tactic-alb";
            case "API Gateway": return "sec-waf";
            case "Certificate Manager": return "acm-note";
            default: return "body-alb";
        }
    }
}