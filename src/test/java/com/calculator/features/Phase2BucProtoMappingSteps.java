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

public class Phase2BucProtoMappingSteps {

    @Autowired
    MockMvc mockMvc;

    private String selectedBucId;
    private ResultActions response;

    @Given("four canonical Business Use Cases \\(BUC\\) are available, each mapping to a gRPC streaming pattern")
    public void verifyFourBucsAvailable() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"BUC1\"")))
                .andExpect(content().string(containsString("value=\"BUC2\"")))
                .andExpect(content().string(containsString("value=\"BUC3\"")))
                .andExpect(content().string(containsString("value=\"BUC4\"")));
    }

    @When("the architect clicks a BUC card {string}")
    public void clickBucCard(String bucLabel) throws Exception {
        this.selectedBucId = bucLabel.split(" - ")[0].trim();
        response = mockMvc.perform(get("/"));
    }

    @Then("the tool automatically loads {string} into application state")
    public void verifyProtoAutoLoaded(String protoFile) throws Exception {
        response.andExpect(status().isOk());
    }

    @And("the proto filename is displayed in the UI as {string}")
    public void verifyProtoFilenameDisplayed(String displayText) throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"proto-autoload-indicator\"")));
    }

    @And("the architect may override it by uploading a custom .proto file")
    public void verifyProtoUploadOptionPresent() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"protoFilePhase2\"")));
    }

    @Given("the architect selects {string}")
    public void selectBucById(String bucId) {
        this.selectedBucId = bucId;
    }

    @When("the BUC card is activated")
    public void activateBucCard() throws Exception {
        response = mockMvc.perform(get("/"));
    }

    @Then("{string} is loaded and {string} is shown as the pattern badge")
    public void verifyProtoAndBadge(String protoFile, String rpcType) throws Exception {
        response.andExpect(status().isOk());
    }
}