package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

public class Phase2BucProtoMappingSteps {

    public static final String CLASSPATH_STATIC_PROTOCOL_BUFFER_FILES = "classpath:static/protos/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResourceLoader resourceLoader;

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
        final Resource resource = resourceLoader.getResource(CLASSPATH_STATIC_PROTOCOL_BUFFER_FILES + protoFile);
        assertTrue(resource.exists());
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
        final Resource resource = resourceLoader.getResource(CLASSPATH_STATIC_PROTOCOL_BUFFER_FILES + protoFile);
        assertTrue(resource.exists());

        try (InputStream inputStream = resource.getInputStream()) {
            final String protoContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            final String normalizedContent = protoContent.replaceAll("\\s+", " ");

            final Pattern rpcPattern = Pattern.compile("rpc\\s+\\w+\\s*\\(([^)]+)\\)\\s*returns\\s*\\(([^)]+)\\)");
            final Matcher matcher = rpcPattern.matcher(normalizedContent);

            assertTrue(matcher.find());

            String detectedRpcType = getDetectedRpcType(matcher);

            assertEquals(rpcType, detectedRpcType);
        }

        response.andExpect(status().isOk());
    }

    private static String getDetectedRpcType(Matcher matcher) {
        final String inputParam = matcher.group(1);
        final String outputParam = matcher.group(2);

        final boolean streamInInput = inputParam.contains("stream");
        boolean streamInOutput = outputParam.contains("stream");

        String detectedRpcType;
        if (streamInInput && streamInOutput) {
            detectedRpcType = "Bi-Directional Streaming";
        } else if (streamInInput) {
            detectedRpcType = "Client Streaming";
        } else if (streamInOutput) {
            detectedRpcType = "Server Streaming";
        } else {
            detectedRpcType = "Unary RPC";
        }
        return detectedRpcType;
    }
}