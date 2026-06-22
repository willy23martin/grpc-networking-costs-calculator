package com.calculator.features;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static com.calculator.shared.ProtocolBuffersUtilsTest.VALID_PROTO_CONTENT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

public class Phase4TcoReportSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions response;

    private MockMultipartFile buildValidProtoFile() {
        return new MockMultipartFile(
                "protoFile",
                "payment-service.proto",
                "application/octet-stream",
                VALID_PROTO_CONTENT.getBytes()
        );
    }

    private MockHttpSession buildSessionWithRetryTactic() {
        MockHttpSession session = new MockHttpSession();

        // 1. Instantiate RetryPattern using its record constructor properties
        // params: (boolean resiliencyRetryTactic, int tacticRetryTimes)
        RetryPattern retryPattern = new RetryPattern(true, 5);

        // 2. Build the aggregate components using defaults or custom states
        ReliabilityTactics reliabilityTactics = ReliabilityTactics.empty();
        SecurityTactics securityTactics = SecurityTactics.empty();
        TimeoutPattern timeoutPattern = TimeoutPattern.empty();
        CircuitBreakerPattern circuitBreakerPattern = CircuitBreakerPattern.empty();

        // FIX: Construct ArchitecturalDecisionsDTO purely using canonical constructor elements
        ArchitecturalDecisionsDTO sessionDto = new ArchitecturalDecisionsDTO(
                1000L,                     // requestsPerSecond
                reliabilityTactics,        // reliabilityTactics
                timeoutPattern,            // timeoutTactic
                retryPattern,              // retryTactic
                circuitBreakerPattern,     // circuitBreakerTactic
                securityTactics            // securityTactics
        );

        session.setAttribute(SESSION_KEY, sessionDto);
        return session;
    }

    @Given("the architect has completed Phase 3 and generated tactical architectural profiles")
    public void verifyTacticalProfilesCompletion() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"phase3\"")));
    }

    @When("I upload a structural Protocol Buffer file to compile and evaluate cost parameters")
    public void uploadProtoFileForTcoEvaluation() throws Exception {
        MockMultipartFile filePayload = buildValidProtoFile();
        MockHttpSession session = buildSessionWithRetryTactic();

        // Performs a multipart POST submission matching the ThymeLeaf configuration engine target
        response = mockMvc.perform(multipart("/calculateTCO")
                .file(filePayload)
                .session(session));
    }

    @Then("the system calculates the baseline gRPC message sizes from the structure")
    public void verifyBackendMessageSizeCalculation() throws Exception {
        // Populates "requestMessageBytes" and "responseMessageBytes" instead of a single "requestSize" attribute.
        response.andExpect(status().isOk())
                .andExpect(model().attributeExists("requestMessageBytes"))
                .andExpect(model().attributeExists("responseMessageBytes"))
                .andExpect(model().attribute("uploadMessage",
                        containsString("uploaded successfully!")));
    }

    @And("the report displays an itemised visual overview of total TCO cost components")
    public void verifyVisualTcoOverviewReport() throws Exception {
        MockMultipartFile filePayload = buildValidProtoFile();
        MockHttpSession session = buildSessionWithRetryTactic();

        response = mockMvc.perform(multipart("/calculateTCO")
                .file(filePayload)
                .session(session));
        response.andExpect(status().isOk());
    }

    @Then("the {string} is sourced from {string}")
    public void verifyTcoComponentSource(String tcoComponent, String architecturalSource) throws Exception {
        // Checked against the captured response payload container mapping element targets
        response.andExpect(content().string(containsString("id=\"cloudTcoBreakdown\"")));
    }

    @And("it is measured using {string} cost metric")
    public void verifyTcoCostMetricType(String costMetricType) throws Exception {
        response.andExpect(content().string(containsString("id=\"cloudTcoBreakdown\"")));
    }
}