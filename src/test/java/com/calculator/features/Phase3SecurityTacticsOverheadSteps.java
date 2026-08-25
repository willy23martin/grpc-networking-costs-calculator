package com.calculator.features;

import com.calculator.infrastructure.web.rest.BaseIntegrationTest;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

public class Phase3SecurityTacticsOverheadSteps extends BaseIntegrationTest {

    private ResultActions response;
    private String securityTactic;

    @Given("the architect enables {string}")
    public void the_architect_enables(String securityTactic) throws Exception {
        this.securityTactic = securityTactic;

        response = mockMvc.perform(get("/api/security/tactic-mappings")
                .accept(MediaType.APPLICATION_JSON));
    }

    @Then("it addresses owasp category {string} and adds {string} to each message")
    public void it_addresses_and_adds_to_each_message(String owaspCategories, String overhead) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[?(@.tacticName == '" + securityTactic + "')]").exists())
                .andExpect(jsonPath("$[?(@.tacticName == '" + securityTactic + "')].owaspLabels[*] ",
                        hasItem(containsString(owaspCategories))))
                .andExpect(jsonPath("$[?(@.tacticName == '" + securityTactic + "')].costFactor",
                        hasItem(containsString(overhead))));
    }
}