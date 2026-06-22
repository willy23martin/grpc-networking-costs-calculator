package com.calculator.features;

import com.calculator.domain.model.quality.TradeoffType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

public class ChooseTacticsToPromoteQualityAttributesSteps {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String targetCharacteristic;
    private String targetTactic;
    private ResultActions apiResultActions;

    @Given("the architect is configured with a base service on the TCO Calculator")
    public void configureBaseService() {
        // Sets up baseline tracking context
    }

    @Given("the architect is navigating Phase 3 {string}")
    public void navigateToPhase3Interface(String phaseName) throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"phase3\"")));
    }

    @Given("I have selected {string} as the primary focus.")
    public void selectArchitecturalCharacteristicContext(String characteristic) {
        this.targetCharacteristic = characteristic;
    }

    @When("I choose {string} as the implementation approach.")
    public void selectSpecificTacticApproach(String tactic) throws Exception {
        this.targetTactic = tactic;
        apiResultActions = mockMvc.perform(get("/api/architecture/quality-definitions"));
    }

    @Then("the system should identify {string} with {string}")
    public void verifyTradeOffImpactAnalysis(String expectedImpactedAttr, String expectedImpactType) throws Exception {
        // 1. Explicitly utilize expectedImpactedAttr parameter to confirm target quality characteristic context
        assertEquals("Affordability", expectedImpactedAttr,
                "This step definition is mapped to evaluate TCO FinOps Affordability drivers.");

        // 2. Extract raw JSON response payload array
        String responseContent = apiResultActions.andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 3. Parse the list of QualityCategoryDefinitions
        JsonNode rootCategories = objectMapper.readTree(responseContent);
        String expectedTacticId = mapTacticToId(targetTactic);

        JsonNode matchingTacticNode = null;
        for (JsonNode category : rootCategories) {
            JsonNode tacticsArray = category.get("tactics");
            if (tacticsArray != null && tacticsArray.isArray()) {
                for (JsonNode tactic : tacticsArray) {
                    if (tactic.has("tacticId") && expectedTacticId.equals(tactic.get("tacticId").asText())) {
                        matchingTacticNode = tactic;
                        break;
                    }
                }
            }
            if (matchingTacticNode != null) break;
        }

        assertNotNull(matchingTacticNode, "Tactic mapping context not found for ID: " + expectedTacticId);

        // 4. Extract description text and perform semantic validation mapping to TradeoffType Enum profiles
        String tradeOffTextDescription = matchingTacticNode.get("tradeoff").asText().toLowerCase();
        String normalizedImpact = expectedImpactType.toUpperCase();

        if (normalizedImpact.startsWith("INHIBIT")) {
            boolean indicatesInhibition = tradeOffTextDescription.contains("overhead") ||
                    tradeOffTextDescription.contains("cost") ||
                    tradeOffTextDescription.contains("charge") ||
                    tradeOffTextDescription.contains("add") ||
                    tradeOffTextDescription.contains("increase") ||
                    tradeOffTextDescription.contains("rps") ||
                    tradeOffTextDescription.contains("fee");

            assertTrue(indicatesInhibition,
                    "Tactic '" + targetTactic + "' matches TradeoffType." + TradeoffType.INHIBITS.name() +
                            " targeting " + expectedImpactedAttr + ", but text fails financial validations. Content: " + tradeOffTextDescription);

        } else if (normalizedImpact.startsWith("ORTHOGONAL")) {
            boolean indicatesOrthogonal = tradeOffTextDescription.contains("negligible") ||
                    tradeOffTextDescription.contains("no direct cost") ||
                    tradeOffTextDescription.contains("bypasses") ||
                    tradeOffTextDescription.contains("client-side") ||
                    tradeOffTextDescription.contains("flat-rated") ||
                    tradeOffTextDescription.contains("no direct cloud cost");

            assertTrue(indicatesOrthogonal,
                    "Tactic '" + targetTactic + "' matches TradeoffType." + TradeoffType.ORTHOGONAL.name() +
                            " targeting " + expectedImpactedAttr + ", but text implies structural cost mutations. Content: " + tradeOffTextDescription);

        } else if (normalizedImpact.startsWith("PROMOTE")) {
            boolean indicatesPromotion = tradeOffTextDescription.contains("saving") ||
                    tradeOffTextDescription.contains("reduce") ||
                    tradeOffTextDescription.contains("optimize");

            assertTrue(indicatesPromotion,
                    "Tactic '" + targetTactic + "' matches TradeoffType." + TradeoffType.PROMOTES.name() +
                            " targeting " + expectedImpactedAttr + ", but text misses efficiency identifiers. Content: " + tradeOffTextDescription);
        }

        // 5. UI View Integration Validation loop
        String htmlTargetId = mapTacticToHtmlId(targetTactic);
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"" + htmlTargetId + "\"")));
    }

    private String mapTacticToId(String tactic) {
        switch (tactic) {
            case "Timeout":                    return "tactic-timeout";
            case "Retry":                      return "tactic-retry";
            case "Circuit Breaker":            return "tactic-cb";
            case "Client-side Load Balancing": return "tactic-client-lb";
            case "Server-side Load Balancing": return "tactic-server-lb";
            case "TLS (One-way)":              return "tactic-tls";
            case "mTLS (Mutual TLS)":          return "tactic-mtls";
            case "OAuth + JWT":                return "tactic-oauth";
            default:                           return "tactic-unknown";
        }
    }

    private String mapTacticToHtmlId(String tactic) {
        switch (tactic) {
            case "Client-side Load Balancing": return "tactic-client-lb";
            case "Server-side Load Balancing": return "tactic-server-lb";
            case "Timeout":                    return "tactic-timeout";
            case "Retry":                      return "tactic-retry";
            case "Circuit Breaker":            return "tactic-cb";
            case "TLS (One-way)":              return "tactic-tls";
            case "mTLS (Mutual TLS)":          return "tactic-mtls";
            case "OAuth + JWT":                return "tactic-oauth";
            default:                           return "cat-cloud-tactics";
        }
    }
}