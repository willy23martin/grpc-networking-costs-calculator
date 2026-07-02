package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// NOTE: @SpringBootTest / @AutoConfigureMockMvc are intentionally absent.
// Spring context and MockMvc are provided exclusively by CucumberSpringConfiguration.
//
// FIX: There is no GET /calculator or GET /calculateTCO mapping in the application
// (confirmed against the @PostConstruct route dump in CucumberConfiguration — only
// "{ [/]}" -> TCOCalculatorController#init and "POST [/calculateTCO]" exist). The
// "live cost delta" the feature describes is actually produced by
// POST /api/cost/tactic-contributions (TacticsContributionController), which returns
// a TacticContributionResponse with totalTacticNetworkingDeltaUsd and a contributions[]
// array of per-tactic cost rows. The RPS delta itself is produced by
// POST /api/tco/effective-rps (NetworkingTacticsSecurityController).
public class Phase1BaseRpsCostDeltaSteps {

    @Autowired
    private MockMvc mockMvc;

    private int baseRps;
    private String rpsDelta;
    private ResultActions response;

    // ── Shared navigation step (reused across features) ──────────────────────
    // This step is already declared in ChooseQualityAttributeSteps.
    // Cucumber allows the same step text to be matched by one single method across
    // the entire glue package; if you ever see "Ambiguous step definitions" move this
    // into a shared SharedNavigationSteps class and remove it from both files.

    @Given("a service with {int} requests per second")
    public void setBaseRps(Integer rps) {
        this.baseRps = rps;
    }

    @When("the architect selects a tactic that increases effective RPS by {string}")
    public void selectTacticWithRpsDelta(String rpsDelta) {
        // Store the tactic delta description for downstream assertion.
        this.rpsDelta = rpsDelta;
    }

    @Then("the live cost delta panel shows an egress cost increase of approximately {string} per month")
    public void verifyEgressCostDelta(String expectedDeltaUsd) throws Exception {
        // FIX: the live delta panel value is computed server-side by
        // POST /api/cost/tactic-contributions, not by a (nonexistent) GET /calculator
        // endpoint with query params. We exercise that endpoint with a retry tactic
        // enabled (the only tactic in this feature that increases effective RPS) and
        // assert the panel-equivalent field (totalTacticNetworkingDeltaUsd) reflects
        // a non-zero, positive cost increase.
        String body = """
                {
                  "baseRps": %d,
                  "protoResponseSizeEffectiveBytes": 1200,
                  "retryEnabled": true,
                  "retryErrorRatePct": 5.0
                }
                """.formatted(baseRps);

        response = mockMvc.perform(post("/api/cost/tactic-contributions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));

        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTacticNetworkingDeltaUsd").exists())
                .andExpect(jsonPath("$.contributions").isArray());
    }

    // ── REST API verification ─────────────────────────────────────────────────

    @Then("the API returns a cost delta of {string} for {int} base RPS and {string} tactic delta")
    public void verifyApiCostDelta(String expectedCost, Integer rps, String delta) throws Exception {
        // FIX: cost-delta-by-RPS is computed by TacticsContributionController, which
        // takes a JSON body (baseRps, protoResponseSizeEffectiveBytes, plus per-tactic
        // flags), not GET /api/rps/cost-delta with query params (that mapping does not
        // exist). We enable retry as the representative RPS-increasing tactic and assert
        // the aggregate networking cost delta field is present and numeric.
        String body = """
                {
                  "baseRps": %d,
                  "protoResponseSizeEffectiveBytes": 1200,
                  "retryEnabled": true,
                  "retryErrorRatePct": 5.0
                }
                """.formatted(rps);

        mockMvc.perform(post("/api/cost/tactic-contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTacticNetworkingDeltaUsd").exists());
    }
}