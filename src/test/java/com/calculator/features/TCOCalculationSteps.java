package com.calculator.features;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static com.calculator.infrastructure.web.rest.helper.TCOCalculatorControllerTestsHelper.noTactics;
import static com.calculator.shared.ProtocolBuffersUtilsTest.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TCOCalculationSteps {

    @Autowired
    private MockMvc mockMvc;

    private String targetedProtoContent;
    private String targetedFilename;
    private int sessionBaselineRps;
    private ResultActions pipelineExecutionResult;

    @Given("the TCO Calculator web application interface is initialized")
    public void theTcoCalculatorWebApplicationInterfaceIsInitialized() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @When("a software architect uploads the schema for {string}")
    public void selectProtoSchemaByUseCase(String useCase) {
        switch (useCase) {
            case "BUC1", "BUC1 with Retry Tactic" -> {
                this.targetedProtoContent = VALID_PROTO_CONTENT;
                this.targetedFilename = "buc1_unary.proto";
            }
            case "BUC2" -> {
                this.targetedProtoContent = VALID_PROTO_CONTENT_BUC2;
                this.targetedFilename = "buc2_server_streaming.proto";
            }
            case "BUC3" -> {
                this.targetedProtoContent = VALID_PROTO_CONTENT_BUC3;
                this.targetedFilename = "buc3_client_streaming.proto";
            }
            case "BUC4" -> {
                this.targetedProtoContent = VALID_PROTO_CONTENT_BUC4;
                this.targetedFilename = "buc4_bidi_streaming.proto";
            }
            default -> throw new IllegalArgumentException("Unknown Business Use Case: " + useCase);
        }
    }

    @When("specifies an operational baseline workload of {int} RPS")
    public void specifiesOperationalBaselineWorkload(int baselineRps) {
        this.sessionBaselineRps = baselineRps;
    }

    @When("configures an active retry pattern policy with {int} max attempts")
    public void executeCalculationPipelineWithParameters(Integer maxAttempts) throws Exception {
        MockMultipartFile filePayload = new MockMultipartFile(
                "protoFile",
                this.targetedFilename,
                "text/plain",
                this.targetedProtoContent.getBytes()
        );

        ArchitecturalDecisionsDTO policyDto;
        if (maxAttempts > 0) {
            policyDto = new ArchitecturalDecisionsDTO(
                    this.sessionBaselineRps,
                    new ReliabilityTactics(false, false),
                    new TimeoutPattern(false, 0),
                    new RetryPattern(true, maxAttempts),
                    new CircuitBreakerPattern(false, 0, 0, 0, 0),
                    SecurityTactics.empty()
            );
        } else {
            policyDto = noTactics(this.sessionBaselineRps);
        }

        // 1. Manually instantiate a mock HTTP Session
        MockHttpSession mockSession = new MockHttpSession();

        // 2. Explicitly bind the DTO using the exact key your production controller looks for
        mockSession.setAttribute(SESSION_KEY, policyDto);

        // 3. Attach the session instance directly to the multipart POST execution
        this.pipelineExecutionResult = mockMvc.perform(multipart("/calculateTCO")
                .file(filePayload)
                .session(mockSession)); // <-- Changed from .sessionAttr() to .session()
    }

    @Then("the calculated processing model should complete without structural parsing errors")
    public void verifyNoParsingErrors() throws Exception {
        this.pipelineExecutionResult.andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"));
    }

    @Then("the application should extract and render calculated baseline payload dimensions")
    public void verifyPayloadDimensionsAreExtracted() throws Exception {
        this.pipelineExecutionResult.andExpect(model().attributeExists("requestSize"))
                .andExpect(model().attributeExists("responseSize"));
    }

    @Then("the evaluation engine must calculate an effective workload demand of {string} RPS")
    public void verifyEffectiveWorkload(String expectedEffectiveRps) throws Exception {
        this.pipelineExecutionResult.andExpect(model().attribute("effectiveRps", expectedEffectiveRps));
    }

    @Then("the final output dashboard should present an adjusted calculation status of {string}")
    public void verifyAdjustedCalculationStatus(String hasTacticsStr) throws Exception {
        boolean expectedHasTactics = Boolean.parseBoolean(hasTacticsStr);
        this.pipelineExecutionResult.andExpect(model().attribute("hasTactics", expectedHasTactics));
    }
}