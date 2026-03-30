package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.dto.tactics.microservices.SAGAPattern;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.dto.tactics.security.authentication.BasicAuthenticationPattern;
import com.calculator.domain.dto.tactics.security.oauth.OAuthTokenValidationMode;
import com.calculator.domain.dto.tactics.security.oauth.jwt.JWTTactic;
import com.calculator.domain.dto.tactics.security.tls.TLSOverhead;
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

    static final String VALID_PROTO_CONTENT = """
                syntax = "proto3";

                package com.ecommerce.order.unary;

                option java_package = "com.ecommerce.order.grpc.unary";
                option java_multiple_files = true;

                import "google/protobuf/timestamp.proto";

                enum OrderStatus {
                  ORDER_STATUS_UNKNOWN = 0;
                  ORDER_STATUS_PENDING = 1;
                  ORDER_STATUS_PROCESSING = 2;
                  ORDER_STATUS_SHIPPED = 3;
                  ORDER_STATUS_DELIVERED = 4;
                  ORDER_STATUS_CANCELLED = 5;
                }

                enum ProductCategory {
                  PRODUCT_CATEGORY_UNKNOWN = 0;
                  PRODUCT_CATEGORY_ELECTRONICS = 1;
                  PRODUCT_CATEGORY_CLOTHING = 2;
                  PRODUCT_CATEGORY_BOOKS = 3;
                  PRODUCT_CATEGORY_HOME_GOODS = 4;
                  PRODUCT_CATEGORY_FOOD = 5;
                }

                message Money {
                  int64 units = 1;
                  int32 nanos = 2;
                }

                message OrderItem {
                  string product_id = 1;
                  int32 quantity = 2;
                  Money unit_price = 3;
                }

                message Address {
                  string street = 1;
                  string city = 2;
                  string state = 3;
                  string zip_code = 4;
                  string country = 5;
                }

                message Order {
                  string order_id = 1;
                  string user_id = 2;
                  repeated OrderItem items = 3;
                  Money total_amount = 4;
                  google.protobuf.Timestamp order_date = 5;
                  OrderStatus status = 6;
                  Address shipping_address = 7;

                  oneof payment_details {
                    string credit_card_token = 8;
                    string paypal_email = 9;
                  }
                }

                message PlaceOrderRequest {
                  string user_id = 1;
                  repeated OrderItem items = 2;
                  Address shipping_address = 3;
                  oneof payment_details {
                    string credit_card_token = 4;
                    string paypal_email = 5;
                  }
                }

                message OrderConfirmation {
                  string order_id = 1;
                  string message = 2;
                  google.protobuf.Timestamp estimated_delivery_date = 3;
                }

                message GetOrderRequest {
                  string order_id = 1;
                }

                service UnaryOrderService {
                  rpc PlaceOrder (PlaceOrderRequest) returns (OrderConfirmation);
                  rpc GetOrderDetails (GetOrderRequest) returns (Order);
                }
                """;

    @Test
    void initPageLoads() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSize",             7509))
                .andExpect(model().attribute("responseSize",            534))
                .andExpect(model().attribute("requestSizeEffective",    7509L))
                .andExpect(model().attribute("responseSizeEffective",   534L))
                .andExpect(model().attribute("requestsPerMonth",        "3B"))
                .andExpect(model().attribute("requestGbPerMonth",       "18,126.6367"))
                .andExpect(model().attribute("responseGbPerMonth",      "1,289.0697"))
                .andExpect(model().attribute("dataTransferCostUsd",     "116.02"))
                .andExpect(model().attribute("hasTactics",              false))
                .andExpect(model().attribute("securityByteOverheadApplied", false));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid_OnLinux() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("requestSize",  7509))
                .andExpect(model().attribute("responseSize", 534));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid_OnWindows() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("requestSize",  7509))
                .andExpect(model().attribute("responseSize", 534));
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid_OnMac() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("requestSize",  7509))
                .andExpect(model().attribute("responseSize", 534));
    }

    // ── Empty / invalid proto ─────────────────────────────────────────────────

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

    // ── Tactics: no-RPS-impact ────────────────────────────────────────────────

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsTacticsSummary_WhenNoRpsImpactTacticsSelected() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true, true),
                new TimeoutPattern(true, 500),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0),
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
                .andExpect(model().attribute("requestsPerMonth",        "3B"))
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
                new SAGAPattern(false, 0, 0, 0),
                SecurityTactics.empty()
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",     true))
                .andExpect(model().attribute("rpsWasAdjusted", false))
                .andExpect(model().attribute("requestsPerMonth", "3B"))
                .andExpect(model().attributeExists("infoTactics"));
    }

    // ── Tactics: Retry ───────────────────────────────────────────────────────
    // effectiveRps = 1000 + (1000 × 3) = 4000
    // requestsPerMonth = 4000 × 2_592_000 = 10_368_000_000

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenRetryIsConfigured() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutPattern(false, 0),
                new RetryPattern(true, 3),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0),
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
                .andExpect(model().attribute("effectiveRps",     "4,000"))
                .andExpect(model().attribute("requestsPerMonth", "10B"))
                .andExpect(model().attributeExists("rpsTactics"))
                .andExpect(model().attribute("securityByteOverheadApplied", false));
    }

    // ── Tactics: SAGA ────────────────────────────────────────────────────────
    // comp=1, ret=1, pivot=1 → 3 steps → effectiveRps = 1000 × 3 = 3000
    // requestsPerMonth = 3000 × 2_592_000 = 7_776_000_000

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfigured() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutPattern(false, 0),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(true, 1, 1, 1),
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
                .andExpect(model().attribute("effectiveRps",     "3,000"))
                .andExpect(model().attribute("requestsPerMonth", "8B"))
                .andExpect(model().attributeExists("rpsTactics"))
                .andExpect(model().attribute("securityByteOverheadApplied", false));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfiguredWithMoreSteps() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutPattern(false, 0),
                new RetryPattern(false, 0),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(true, 2, 3, 1),
                SecurityTactics.empty()
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",       true))
                .andExpect(model().attribute("rpsWasAdjusted",   true))
                .andExpect(model().attribute("effectiveRps",     "6,000"))
                .andExpect(model().attribute("requestsPerMonth", "16B"));
    }

    // ── Tactics: SAGA + Retry combined ───────────────────────────────────────
    // SAGA first (×3), then Retry (+2000)
    // effectiveRps = (1000 × 3) + (1000 × 2) = 5000
    // requestsPerMonth = 5000 × 2_592_000 = 12_960_000_000

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaAndRetryAreCombined() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutPattern(false, 0),
                new RetryPattern(true, 2),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(true, 1, 1, 1),
                SecurityTactics.empty()
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",       true))
                .andExpect(model().attribute("rpsWasAdjusted",   true))
                .andExpect(model().attribute("effectiveRps",     "5,000"))
                .andExpect(model().attribute("requestsPerMonth", "13B"))
                .andExpect(model().attributeExists("infoTactics"))
                .andExpect(model().attributeExists("rpsTactics"));
    }

    // ── Tactics: mixed info + RPS ─────────────────────────────────────────────

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsTacticsSummary_WhenMixedTacticsSelected() throws Exception {
        ArchitecturalDecisionsDTO dto = new ArchitecturalDecisionsDTO(
                1000,
                new ReliabilityTactics(true, false),
                new TimeoutPattern(true, 300),
                new RetryPattern(true, 3),
                new CircuitBreakerPattern(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0),
                SecurityTactics.empty()
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, dto))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",       true))
                .andExpect(model().attribute("rpsWasAdjusted",   true))
                .andExpect(model().attribute("effectiveRps",     "4,000"))
                .andExpect(model().attribute("requestsPerMonth", "10B"))
                .andExpect(model().attributeExists("infoTactics"))
                .andExpect(model().attributeExists("rpsTactics"));
    }

    // ── OS-specific SAGA ──────────────────────────────────────────────────────

    @Test
    @EnabledOnOs(OS.LINUX)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfigured_OnLinux() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, sagaOnly(1, 1, 1)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("effectiveRps",     "3,000"))
                .andExpect(model().attribute("requestsPerMonth", "8B"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfigured_OnWindows() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, sagaOnly(1, 1, 1)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("effectiveRps",     "3,000"))
                .andExpect(model().attribute("requestsPerMonth", "8B"));
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfigured_OnMac() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, sagaOnly(1, 1, 1)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("effectiveRps",     "3,000"))
                .andExpect(model().attribute("requestsPerMonth", "8B"));
    }

    // ── Security: TLS only, 0 reconnects ─────────────────────────────────────
    // TLS adds 29 bytes (typical, RFC 8446) to every request and response frame.
    // 0 reconnects → handshakeExtraRps = round(0 × 2 / 3600) = 0 → rpsWasAdjusted = false
    // effectiveRequestSize  = 7509 + 29 = 7538
    // effectiveResponseSize = 534  + 29 = 563
    // requestsPerMonth      = 1000 × 2_592_000 = 2_592_000_000
    // responseGbPerMonth    = 563 × 1000 × 2_592_000 / 1_073_741_824 ≈ 1.359,0753
    // dataTransferCostUsd   ≈ 122,32

    @Test
    void calculateProtoFileTCONetworkingCosts_AppliesTlsByteOverhead_WhenTlsIsEnabled() throws Exception {
        SecurityTactics tls = new SecurityTactics(
                new TLSTactic(true, false, 0),
                new JWTTactic(false, OAuthTokenValidationMode.LOCAL, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(false)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, tls)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSize",             7509))
                .andExpect(model().attribute("responseSize",            534))
                .andExpect(model().attribute("requestSizeEffective",    7539L))
                .andExpect(model().attribute("responseSizeEffective",   564L))
                .andExpect(model().attribute("tlsOverheadBytes",        TLSOverhead.RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX.getOverhead()))
                .andExpect(model().attribute("jwtOverheadBytes",        0))
                .andExpect(model().attribute("securityByteOverheadApplied", true))
                .andExpect(model().attribute("rpsWasAdjusted",          false))
                .andExpect(model().attribute("requestsPerMonth",        "3B"))
                .andExpect(model().attribute("responseGbPerMonth",      "1,361.4893"))
                .andExpect(model().attribute("dataTransferCostUsd",     "122.53"))
                .andExpect(model().attribute("hasTactics",              true))
                .andExpect(model().attributeExists("infoTactics"));
    }

    // ── Security: mTLS, 0 reconnects ─────────────────────────────────────────
    // Per-message overhead identical to TLS (same RFC 8446 record format).
    // 0 reconnects → no extra RPS. Expected results identical to TLS-only.

    @Test
    void calculateProtoFileTCONetworkingCosts_AppliesMtlsByteOverhead_WhenMtlsIsEnabled() throws Exception {
        SecurityTactics mtls = new SecurityTactics(
                new TLSTactic(true, true, 0),
                new JWTTactic(false, OAuthTokenValidationMode.LOCAL, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(false)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, mtls)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSizeEffective",    7539L))
                .andExpect(model().attribute("responseSizeEffective",   564L))
                .andExpect(model().attribute("tlsOverheadBytes",        TLSOverhead.RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX.getOverhead()))
                .andExpect(model().attribute("securityByteOverheadApplied", true))
                .andExpect(model().attribute("rpsWasAdjusted",          false))
                .andExpect(model().attribute("responseGbPerMonth",      "1,361.4893"))
                .andExpect(model().attribute("dataTransferCostUsd",     "122.53"))
                .andExpect(model().attribute("hasTactics",              true));
    }

    // ── Security: mTLS with high reconnects generating extra RPS ─────────────
    // 720 reconnects/hr × 5 mTLS messages / 3600 = exactly 1 extra req/s
    // effectiveRps = 1001 → rpsWasAdjusted = true
    // requestsPerMonth = 1001 × 2_592_000 = 2_594_592_000
    // responseGbPerMonth = 563 × 1001 × 2_592_000 / 1_073_741_824 ≈ 1.360,4344

    @Test
    void calculateProtoFileTCONetworkingCosts_AddsHandshakeRps_WhenMtlsReconnectRateIsHigh() throws Exception {
        SecurityTactics mtls = new SecurityTactics(
                new TLSTactic(true, true, 720),
                new JWTTactic(false, OAuthTokenValidationMode.LOCAL, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(false)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, mtls)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("rpsWasAdjusted",          true))
                .andExpect(model().attribute("effectiveRps",            "1,001"))
                .andExpect(model().attribute("requestsPerMonth",        "3B"))
                .andExpect(model().attribute("requestSizeEffective",    7539L))
                .andExpect(model().attribute("responseSizeEffective",   564L))
                .andExpect(model().attribute("securityByteOverheadApplied", true))
                .andExpect(model().attribute("responseGbPerMonth",      "1,362.8508"))
                .andExpect(model().attribute("dataTransferCostUsd",     "122.66"))
                .andExpect(model().attributeExists("rpsTactics"));
    }

    // ── Security: OAuth 2.0 + JWT, local validation ───────────────────────────
    // JWT adds 650 bytes to requests only (Authorization metadata header). No TLS.
    // tokenAcquisitionExtraRps = round(1000 / (3600 × 1)) = 0 → rpsWasAdjusted = false
    // effectiveRequestSize  = 7509 + 650 = 8159
    // effectiveResponseSize = 534         (unchanged — JWT on requests only)
    // responseGbPerMonth unchanged from baseline → cost unchanged at 116,02

    @Test
    void calculateProtoFileTCONetworkingCosts_AppliesJwtByteOverhead_WhenOAuthLocalValidationIsEnabled() throws Exception {
        SecurityTactics oauth = new SecurityTactics(
                new TLSTactic(false, false, 0),
                new JWTTactic(true, OAuthTokenValidationMode.LOCAL, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(false)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, oauth)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSize",             7509))
                .andExpect(model().attribute("responseSize",            534))
                .andExpect(model().attribute("requestSizeEffective",    8159L))
                .andExpect(model().attribute("responseSizeEffective",   534L))
                .andExpect(model().attribute("tlsOverheadBytes",        0))
                .andExpect(model().attribute("jwtOverheadBytes",        650))
                .andExpect(model().attribute("securityByteOverheadApplied", true))
                .andExpect(model().attribute("rpsWasAdjusted",          false))
                .andExpect(model().attribute("requestsPerMonth",        "3B"))
                .andExpect(model().attribute("responseGbPerMonth",      "1,289.0697"))
                .andExpect(model().attribute("dataTransferCostUsd",     "116.02"))
                .andExpect(model().attribute("requestGbPerMonth",       "19,695.7290"))
                .andExpect(model().attribute("hasTactics",              true))
                .andExpect(model().attributeExists("infoTactics"));
    }

    // ── Security: OAuth 2.0 + JWT, remote introspection ──────────────────────
    // Remote introspection adds 1 extra req/s per incoming gRPC request.
    // tokenAcquisitionExtraRps = round(1000 / (3600 × 1)) = 0
    // effectiveRps = 1000 + 1000 (introspection) = 2000 → rpsWasAdjusted = true
    // effectiveRequestSize = 8159, effectiveResponseSize = 534
    // requestsPerMonth = 2000 × 2_592_000 = 5_184_000_000
    // responseGbPerMonth = 534 × 2000 × 2_592_000 / 1_073_741_824 ≈ 2.578,1393
    // dataTransferCostUsd ≈ 232,03

    @Test
    void calculateProtoFileTCONetworkingCosts_AddsIntrospectionRps_WhenOAuthRemoteIntrospectionIsEnabled() throws Exception {
        SecurityTactics oauth = new SecurityTactics(
                new TLSTactic(false, false, 0),
                new JWTTactic(true, OAuthTokenValidationMode.REMOTE_INTROSPECTION, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(false)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, oauth)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("rpsWasAdjusted",          true))
                .andExpect(model().attribute("effectiveRps",            "2,000"))
                .andExpect(model().attribute("requestsPerMonth",        "5B"))
                .andExpect(model().attribute("requestSizeEffective",    8159L))
                .andExpect(model().attribute("responseSizeEffective",   534L))
                .andExpect(model().attribute("jwtOverheadBytes",        650))
                .andExpect(model().attribute("securityByteOverheadApplied", true))
                .andExpect(model().attribute("responseGbPerMonth",      "2,578.1393"))
                .andExpect(model().attribute("dataTransferCostUsd",     "232.03"))
                .andExpect(model().attributeExists("rpsTactics"));
    }

    // ── Security: TLS + OAuth local combined ─────────────────────────────────
    // TLS adds 29 bytes to both frames. JWT adds 650 bytes to requests only.
    // effectiveRequestSize  = 7509 + 29 + 650 = 8188
    // effectiveResponseSize = 534  + 29       = 563
    // effectiveRps = 1000 (0 reconnects, local validation, tokenAcq = 0)
    // requestGbPerMonth  = 8188 × 1000 × 2_592_000 / 1_073_741_824 ≈ 19.765,7347
    // responseGbPerMonth = 563  × 1000 × 2_592_000 / 1_073_741_824 ≈  1.359,0753

    @Test
    void calculateProtoFileTCONetworkingCosts_AppliesBothTlsAndJwtOverhead_WhenCombined() throws Exception {
        SecurityTactics combined = new SecurityTactics(
                new TLSTactic(true, false, 0),
                new JWTTactic(true, OAuthTokenValidationMode.LOCAL, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(false)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, combined)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSizeEffective",    8189L))
                .andExpect(model().attribute("responseSizeEffective",   564L))
                .andExpect(model().attribute("tlsOverheadBytes", TLSOverhead.RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX.getOverhead()))
                .andExpect(model().attribute("jwtOverheadBytes",        650))
                .andExpect(model().attribute("securityByteOverheadApplied", true))
                .andExpect(model().attribute("rpsWasAdjusted",          false))
                .andExpect(model().attribute("requestsPerMonth",        "3B"))
                .andExpect(model().attribute("requestGbPerMonth",       "19,768.1487"))
                .andExpect(model().attribute("responseGbPerMonth",      "1,361.4893"))
                .andExpect(model().attribute("dataTransferCostUsd",     "122.53"));
    }

    // ── Security: Basic Authentication ───────────────────────────────────────
    // Basic Auth is informational only — no byte overhead model, no extra RPS.
    // securityByteOverheadApplied = false, rpsWasAdjusted = false.
    // hasTactics = true (appears in infoTactics with the production warning).

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsBasicAuthAsInformational_WhenBasicAuthIsEnabled() throws Exception {
        SecurityTactics basicAuth = new SecurityTactics(
                new TLSTactic(false, false, 0),
                new JWTTactic(false, OAuthTokenValidationMode.LOCAL, 3600, 1,
                        InterceptorType.UNARY), new BasicAuthenticationPattern(true)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000, basicAuth)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSizeEffective",    7509L))
                .andExpect(model().attribute("responseSizeEffective",   534L))
                .andExpect(model().attribute("tlsOverheadBytes",        0))
                .andExpect(model().attribute("jwtOverheadBytes",        0))
                .andExpect(model().attribute("securityByteOverheadApplied", false))
                .andExpect(model().attribute("rpsWasAdjusted",          false))
                .andExpect(model().attribute("requestsPerMonth",        "3B"))
                .andExpect(model().attribute("dataTransferCostUsd",     "116.02"))
                .andExpect(model().attribute("hasTactics",              true))
                .andExpect(model().attributeExists("infoTactics"));
    }

}