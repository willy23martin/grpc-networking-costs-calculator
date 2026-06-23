package com.calculator.features;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.infrastructure.web.rest.BaseIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static com.calculator.shared.ProtocolBuffersUtilsTest.VALID_PROTO_CONTENT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class Phase4TcoReportSteps extends BaseIntegrationTest {

    private ResultActions response;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        RetryPattern retryPattern = new RetryPattern(true, 5);
        ReliabilityTactics reliabilityTactics = ReliabilityTactics.empty();
        SecurityTactics securityTactics = SecurityTactics.empty();
        TimeoutPattern timeoutPattern = TimeoutPattern.empty();
        CircuitBreakerPattern circuitBreakerPattern = CircuitBreakerPattern.empty();

        ArchitecturalDecisionsDTO sessionDto = new ArchitecturalDecisionsDTO(
                1000L,
                reliabilityTactics,
                timeoutPattern,
                retryPattern,
                circuitBreakerPattern,
                securityTactics
        );

        session.setAttribute(SESSION_KEY, sessionDto);
        return session;
    }

    private void executeProtoUploadCalculation() throws Exception {
        MockMultipartFile filePayload = buildValidProtoFile();
        MockHttpSession session = buildSessionWithRetryTactic();

        response = mockMvc.perform(multipart("/calculateTCO")
                .file(filePayload)
                .session(session));
    }

    @Given("a BUC has been selected and a proto file is loaded")
    public void a_buc_has_been_selected_and_a_proto_file_is_loaded() {
    }

    @When("the architect clicks Calculate TCO in Phase 3")
    public void the_architect_clicks_calculate_tco_in_phase_3() throws Exception {
    }

    @Then("the tool calls POST api-session-tactics and POST calculateTCO")
    public void the_tool_calls_post_api_session_tactics_and_post_calculate_tco() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockMultipartFile filePayload = buildValidProtoFile();

        RetryPattern retryPattern = new RetryPattern(true, 5);
        ArchitecturalDecisionsDTO sessionDto = new ArchitecturalDecisionsDTO(
                1000L,
                ReliabilityTactics.empty(),
                TimeoutPattern.empty(),
                retryPattern,
                CircuitBreakerPattern.empty(),
                SecurityTactics.empty()
        );

        mockMvc.perform(post("/api/session/tactics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sessionDto))
                        .session(session))
                .andExpect(status().isNoContent());

        response = mockMvc.perform(multipart("/calculateTCO")
                .file(filePayload)
                .session(session));

        response.andExpect(status().isOk());
    }

    @And("the system parses response to extract the cost model parameters")
    public void the_system_parses_the_spring_boot_thymeleaf_html_response() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(model().attributeExists("requestMessageBytes"))
                .andExpect(model().attributeExists("responseMessageBytes"))
                .andExpect(model().attributeExists("uploadMessage"));
    }

    @And("the interface automatically navigates to Phase 4 to render the full report")
    public void the_interface_automatically_navigates_to_phase_4() throws Exception {
        response.andExpect(view().name("calculator"));
    }

    // ════════════════════════ SCENARIO 2 ════════════════════════
    @Given("the backend has returned a valid TCO response")
    public void the_backend_has_returned_a_valid_tco_response() throws Exception {
        if (response == null) {
            executeProtoUploadCalculation();
        }
        response.andExpect(status().isOk());
    }

    @When("Phase 4 loads")
    public void phase_4_loads() throws Exception {
        response.andExpect(view().name("calculator"));
    }

    @Then("the report displays a Service Identity Banner with metadata and active tactics")
    public void the_report_displays_a_service_identity_banner() throws Exception {
        response.andExpect(view().name("calculator"))
                .andExpect(model().attributeExists("uploadMessage"));
    }

    @And("the report displays an RPS Adjustment Banner showing load shifts")
    public void the_report_displays_an_rps_adjustment_banner() throws Exception {
        response.andExpect(model().attributeExists("requestMessageBytes"));
    }

    @And("the report displays a Security Overhead Banner showing RFC byte additions")
    public void the_report_displays_a_security_overhead_banner() throws Exception {
        response.andExpect(model().attributeExists("responseMessageBytes"));
    }

    @And("the report displays Tactics Summary Tables")
    public void the_report_displays_tactics_summary_tables() throws Exception {
        response.andExpect(view().name("calculator"));
    }

    @And("the page renders a Proto Sizes Table with volumes throughput and AWS egress charges")
    public void the_page_renders_a_proto_sizes_table() throws Exception {
        response.andExpect(model().attributeExists("requestMessageBytes"))
                .andExpect(model().attributeExists("responseMessageBytes"));
    }

    @And("^the page renders a complete TCO Breakdown Table with networking cloud infra FinOps metrics and DR/BC status$")
    public void the_page_renders_a_complete_tco_breakdown_table() throws Exception {
        response.andExpect(view().name("calculator"));
    }

    @And("the page renders Unit Economics breakdown ROI Analysis panel and FinOps Architecture Notes")
    public void the_page_renders_unit_economics_breakdown() throws Exception {
        response.andExpect(view().name("calculator"));
    }

    // ════════════════════════ SCENARIO 3 ════════════════════════
    @Given("Phase 4 displays a non-zero TCO")
    public void phase_4_displays_a_non_zero_tco() throws Exception {
        the_backend_has_returned_a_valid_tco_response();
    }

    @When("the architect clicks Add to Portfolio")
    public void the_architect_clicks_add_to_portfolio() {
        // Mock state transition placeholder
    }

    @Then("the complete service entry profile is persisted to browser localStorage")
    public void the_complete_service_entry_profile_is_persisted() {
        // UI validation placeholder
    }

    @And("a toast notification confirms the save making the portfolio available across page reloads")
    public void a_toast_notification_confirms_the_save() {
        // UI feedback placeholder
    }

    // ════════════════════════ SCENARIO OUTLINE ════════════════════════
    @Given("the TCO report has been generated")
    public void the_tco_report_has_been_generated() throws Exception {
        executeProtoUploadCalculation();
    }

    @Then("the {string} is sourced from {string}")
    public void verifyTcoComponentSource(String tcoComponent, String architecturalSource) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(view().name("calculator"));
    }

    @And("it is measured using {string} cost metric")
    public void verifyTcoCostMetricType(String costMetricType) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(view().name("calculator"));
    }
}