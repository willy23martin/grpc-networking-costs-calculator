package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.model.architecture.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.dto.tactics.security.authentication.BasicAuthenticationPattern;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import com.calculator.domain.dto.tactics.security.oauth.jwt.JWTTactic;
import com.calculator.domain.dto.tactics.security.tls.TLSTactic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static com.calculator.infrastructure.web.rest.helper.TCOCalculatorControllerTestsHelper.*;
import static com.calculator.shared.ProtocolBuffersUtilsTest.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TCOCalculatorControllerTest {

    private final MockMvc mockMvc;

    @Autowired
    TCOCalculatorControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void initPageLoads() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_BUC1_Unary_ShowsCosts() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("requestSize"))
                .andExpect(model().attributeExists("responseSize"))
                .andExpect(model().attribute("hasTactics", false));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_BUC2_ServerStreaming_ShowsCosts() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(new MockMultipartFile("protoFile", "buc2_search.proto", "text/plain", VALID_PROTO_CONTENT_BUC2.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(500)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("requestSize"))
                .andExpect(model().attributeExists("responseSize"))
                .andExpect(model().attributeExists("dataTransferCostUsd"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_BUC3_ClientStreaming_ShowsCosts() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(new MockMultipartFile("protoFile", "buc3_batch.proto", "text/plain", VALID_PROTO_CONTENT_BUC3.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(800)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("requestSize"))
                .andExpect(model().attributeExists("responseSize"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_BUC4_BiDirectionalStreaming_ShowsCosts() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(new MockMultipartFile("protoFile", "buc4_bidi.proto", "text/plain", VALID_PROTO_CONTENT_BUC4.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1200)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("requestSize"))
                .andExpect(model().attributeExists("responseSize"))
                .andExpect(model().attributeExists("dataTransferCostUsd"));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid_OnLinux() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("requestSize"))
                .andExpect(model().attributeExists("responseSize"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid_OnWindows() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("requestSize"))
                .andExpect(model().attributeExists("responseSize"));
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid_OnMac() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("requestSize"))
                .andExpect(model().attributeExists("responseSize"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsError_WhenProtoFileIsEmpty() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(new MockMultipartFile("protoFile", "empty.proto", "text/plain", new byte[0]))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("uploadMessage", "No file selected for upload."));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsError_WhenProtoFileHasNoRpcDefinition() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(new MockMultipartFile("protoFile", "no_rpc.proto", "text/plain", """
                                syntax = "proto3";
                                message PlaceOrderRequest { string user_id = 1; }
                                message OrderConfirmation  { string order_id = 1; }
                                """.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error",
                        "Could not find a valid RPC definition to extract Request and Response message types from the .proto file."));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsTacticsSummary_WhenNoRpsImpactTacticsSelected() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true, true),
                new TimeoutPattern(true, 500),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                SecurityTactics.empty()
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",              true))
                .andExpect(model().attribute("rpsWasAdjusted",          false))
                .andExpect(model().attribute("baseRps",                 "1,000"))
                .andExpect(model().attribute("effectiveRps",            "1,000"))
                .andExpect(model().attributeExists("infoTactics"))
                .andExpect(model().attribute("securityByteOverheadApplied", false));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsTacticsSummary_WhenCircuitBreakerIsConfigured() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutPattern(false, 0),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(true, 10, 5, 60000, 50),
                SecurityTactics.empty()
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",     true))
                .andExpect(model().attribute("rpsWasAdjusted", false))
                .andExpect(model().attributeExists("infoTactics"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenRetryIsConfigured() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutPattern(false, 0),
                new RetryPattern(true, 3),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                SecurityTactics.empty()
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",       true))
                .andExpect(model().attribute("rpsWasAdjusted",   true))
                .andExpect(model().attribute("baseRps",          "1,000"))
                .andExpect(model().attribute("effectiveRps",     "1,030"))
                .andExpect(model().attributeExists("rpsTactics"))
                .andExpect(model().attribute("securityByteOverheadApplied", false));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_AppliesTlsByteOverhead_WhenTlsIsEnabled() throws Exception {
        SecurityTactics tls = new SecurityTactics(
                new TLSTactic(true, false, 0),
                new JWTTactic(false, OAuthTokenValidationModes.LOCAL, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(false)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, tls)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("securityByteOverheadApplied", true))
                .andExpect(model().attribute("rpsWasAdjusted",          false))
                .andExpect(model().attribute("hasTactics",              true))
                .andExpect(model().attributeExists("infoTactics"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_AppliesJwtByteOverhead_WhenOAuthLocalValidationIsEnabled() throws Exception {
        SecurityTactics oauth = new SecurityTactics(
                new TLSTactic(false, false, 0),
                new JWTTactic(true, OAuthTokenValidationModes.LOCAL, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(false)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, oauth)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("securityByteOverheadApplied", true))
                .andExpect(model().attribute("rpsWasAdjusted",          false))
                .andExpect(model().attribute("hasTactics",              true))
                .andExpect(model().attributeExists("infoTactics"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_AddsIntrospectionRps_WhenOAuthRemoteIntrospectionIsEnabled() throws Exception {
        SecurityTactics oauth = new SecurityTactics(
                new TLSTactic(false, false, 0),
                new JWTTactic(true, OAuthTokenValidationModes.REMOTE_INTROSPECTION, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(false)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, oauth)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("rpsWasAdjusted",          true))
                .andExpect(model().attribute("effectiveRps",            "2,000"))
                .andExpect(model().attributeExists("rpsTactics"));
    }
}