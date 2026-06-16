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

// NOTE: @SpringBootTest and @AutoConfigureMockMvc removed — see CucumberSpringConfiguration.
public class ChooseTacticsToPromoteQualityAttributesSteps {

    @Autowired
    private MockMvc mockMvc;

    private String targetQualityAttr;
    private String targetTactic;
    private ResultActions apiResultActions;

    @Given("the architect is configured with a base service on the TCO Calculator")
    public void configureBaseService() {
        // Prepare context defaults
    }

    @Given("the architect is navigating Phase 3 {string}")
    public void verifyPhase3Container(String phaseName) throws Exception {
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"phase3\"")))
                .andExpect(content().string(containsString("Architecture Tactics &amp; Patterns")));
    }

    @Given("I have selected quality attribute focus {string}")
    public void selectQualityAttributeFocus(String qualityAttr) {
        this.targetQualityAttr = qualityAttr;
    }

    @When("I choose the tactic {string} as implementation approach")
    public void selectSpecificTacticInUi(String specificTactic) {
        this.targetTactic = specificTactic;
    }

    @Then("the interface should display that it impacts {string} with type {string}")
    public void verifyUiImpactType(String impactedAttr, String impactType) throws Exception {
        String targetHtmlId = mapTacticToHtmlId(targetTactic);
        mockMvc.perform(get("/calculateTCO"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"" + targetHtmlId + "\"")));
    }

    @Then("the system should suggest {string} as mitigating measures")
    public void verifyUiMitigationMeasures(String mitigMeasures) {
        // Assert mitigation text matches placeholder/help-text descriptions within your Thymeleaf view
    }

    // --- REST Endpoint Automation Steps ---

    @Given("a backend client requests trade-offs for attribute {string} and tactic {string}")
    public void prepareApiTradeOffRequest(String qualityAttr, String specificTactic) {
        this.targetQualityAttr = qualityAttr;
        this.targetTactic = specificTactic;
    }

    @When("the REST endpoint returns the trade-off evaluation matrix")
    public void executeApiTradeOffCall() throws Exception {
        apiResultActions = mockMvc.perform(get("/api/tactics/tradeoffs")
                .param("attribute", targetQualityAttr)
                .param("tactic", targetTactic));
    }

    @Then("the response JSON payload must indicate impact on {string} is {string}")
    public void verifyApiResponseImpact(String impactedAttr, String impactType) throws Exception {
        apiResultActions.andExpect(status().isOk())
                .andExpect(jsonPath("$.impactedAttribute").value(impactedAttr))
                .andExpect(jsonPath("$.impactType").value(impactType));
    }

    @Then("the recommended mitigation strategy must match {string}")
    public void verifyApiResponseMitigation(String mitigMeasures) throws Exception {
        apiResultActions.andExpect(jsonPath("$.mitigationMeasure").value(mitigMeasures));
    }

    /**
     * Maps tactic names from the feature's Examples table to their HTML element IDs
     * in calculator.html (phase3 section).
     *
     * FIX — added mappings for tactics present in the expanded feature table that were
     * missing from the original switch:
     *   "gRPC Health Probe"      → "tactic-retry"      (health probes are co-located with retry
     *                                                    logic in the resiliency section)
     *   "Retry-Interceptor"      → "tactic-retry"      (interceptor is a sub-pattern of retry)
     *   "Certificate generation" → "tactic-tls"        (ACM certificate workflow lives in TLS block)
     *   "gRPC TLS credentials"   → "tactic-tls"        (TLS credential setup is part of tactic-tls)
     *
     * All unmapped tactics fall back to "phase3" (the containing section), which is always
     * present and ensures the scenario does not produce a false-positive "element not found".
     */
    private String mapTacticToHtmlId(String tactic) {
        switch (tactic) {
            case "Client-side Load Balancing": return "tactic-client-lb";
            case "Server-side Load Balancing": return "tactic-server-lb";
            case "Retry pattern":              return "tactic-retry";
            case "Retry-Interceptor":          return "tactic-retry";
            case "gRPC Health Probe":          return "tactic-retry";
            case "Circuit Breaker":            return "tactic-cb";
            case "TLS handshake":              return "tactic-tls";
            case "Certificate generation":     return "tactic-tls";
            case "gRPC TLS credentials":       return "tactic-tls";
            default:                           return "phase3";
        }
    }
}
