package com.calculator.features;

import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.infrastructure.web.rest.BaseIntegrationTest;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ChooseTacticsToPromoteQualityAttributesSteps extends BaseIntegrationTest {

    private String architecturalCharacteristic;
    private String specificTactic;

    @Given("I have selected {string} as the primary focus")
    public void setPrimaryFocus(String architecturalCharacteristic) {
        this.architecturalCharacteristic = architecturalCharacteristic;
    }

    @When("I choose {string} as the implementation approach")
    public void chooseImplementationApproach(String specificTactic) {
        this.specificTactic = specificTactic;
    }

    @Then("the system should identify {string} with {string}")
    public void verifyTacticImpact(String expectedValue, String impactType) throws Exception {
        String targetApiPath = getEndpointForCharacteristic(architecturalCharacteristic);

        String jsonPath;
        if (TradeoffType.PROMOTES.name().equals(impactType)) {
            jsonPath = "$[?(@.tacticName == '" + specificTactic + "')].tacticCategory";
        } else {
            jsonPath = "$[?(@.tacticName == '" + specificTactic + "')].impactedAttribute";
        }

        mockMvc.perform(get(targetApiPath))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(jsonPath).value(expectedValue));

        if (TradeoffType.INHIBITS.name().equals(impactType)) {
            mockMvc.perform(get(targetApiPath))
                    .andExpect(jsonPath("$[?(@.tacticName == '" + specificTactic + "')].impactType").value(impactType));
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