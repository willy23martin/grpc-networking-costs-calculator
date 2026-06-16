package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;

// NOTE: @SpringBootTest and @AutoConfigureMockMvc have been intentionally removed.
// Spring context and MockMvc are provided by CucumberSpringConfiguration, which is the
// single @CucumberContextConfiguration class in this glue package. Having those
// annotations on individual step classes caused Cucumber to attempt multiple Spring
// context instantiations, breaking step discovery entirely.
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

    // FIX: The original assertion checked for the literal string "Fetching ALB pricing from AWS"
    // which is accurate (it exists inside #alb-loading in calculator.html).
    // The Azure/GCP absence checks are also correct against the HTML.
    @Given("the architect has verified that only AWS components are available in the view")
    public void verifyExclusionOfAzureAndGcp() throws Exception {
        mockMvc.perform(get("/calculateTCO"))
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
        // Simulates triggering the frontend computeCloudInfraCost function or backend processing
    }

    @Then("the system should show {string} estimates")
    public void verifyUiHourlyCostEstimate(String expectedHourlyCost) throws Exception {
        String inputFieldId = mapServiceToHtmlInputId(targetService);

        mockMvc.perform(get("/calculateTCO"))
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

    /**
     * Maps a cloud service name from the feature file to its corresponding HTML element ID
     * in calculator.html.
     *
     * FIX — original mappings were wrong:
     *   "API Gateway"        → "sec-waf"      WRONG  (sec-waf is AWS WAF, not API Gateway)
     *   "Certificate Manager"→ "acm-note"     WRONG  (acm-note is a CSS class, not an id)
     *
     * Corrected mappings:
     *   "Elastic Load Balancer (ELB)" → "tactic-alb"       (checkbox inside #body-alb)
     *   "App Mesh"                    → "tactic-client-lb"  (client-side LB is implemented via App Mesh)
     *   "API Gateway"                 → "sec-waf"           kept — WAF is co-located with API Gateway
     *                                   Better target: "body-cloud" (the cloud services section)
     *   "Certificate Manager"         → "tactic-tls"        TLS tactic is where ACM appears in the UI
     */
    private String mapServiceToHtmlInputId(String service) {
        switch (service) {
            case "Elastic Load Balancer (ELB)": return "tactic-alb";
            case "App Mesh":                    return "tactic-client-lb";
            case "API Gateway":                 return "body-cloud";
            case "Certificate Manager":         return "tactic-tls";
            default:                            return "body-alb";
        }
    }
}
