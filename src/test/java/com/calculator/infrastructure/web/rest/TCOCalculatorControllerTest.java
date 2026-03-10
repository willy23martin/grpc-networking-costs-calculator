package com.calculator.infrastructure.web.rest;

import com.calculator.domain.model.tactics.TacticsConfigDTO;
import com.calculator.domain.model.tactics.microservices.SAGAPattern;
import com.calculator.domain.model.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.model.tactics.resiliency.CircuitBreakerTactic;
import com.calculator.domain.model.tactics.resiliency.RetryTactic;
import com.calculator.domain.model.tactics.resiliency.TimeoutTactic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
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
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSize",        7509))
                .andExpect(model().attribute("responseSize",       534))
                .andExpect(model().attribute("requestsPerMonth",   "2.592.000.000"))
                .andExpect(model().attribute("requestGbPerMonth",  "18126,6367"))
                .andExpect(model().attribute("responseGbPerMonth", "1289,0697"))
                .andExpect(model().attribute("dataTransferCostUsd","116,02"))
                .andExpect(model().attribute("hasTactics",         false));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid_OnLinux() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSize",  7509))
                .andExpect(model().attribute("responseSize", 534));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid_OnWindows() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSize",  7509))
                .andExpect(model().attribute("responseSize", 534));
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid_OnMac() throws Exception {
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("requestSize",  7509))
                .andExpect(model().attribute("responseSize", 534));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsError_WhenProtoFileIsEmpty() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "protoFile", "empty.proto", "text/plain", new byte[0]);
        mockMvc.perform(multipart("/calculateTCO")
                        .file(emptyFile)
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("uploadMessage", "No file selected for upload."));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsError_WhenProtoFileHasNoRpcDefinition() throws Exception {
        MockMultipartFile noRpcProtoFile = new MockMultipartFile(
                "protoFile", "no_rpc.proto", "text/plain",
                """
                syntax = "proto3";
                option java_package = "com.ecommerce.order.grpc.unary";
                option java_multiple_files = true;
                message PlaceOrderRequest { string user_id = 1; }
                message OrderConfirmation { string order_id = 1; }
                """.getBytes());
        mockMvc.perform(multipart("/calculateTCO")
                        .file(noRpcProtoFile)
                        .sessionAttr(SESSION_KEY, noTactics(1000)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("error",
                        "Could not find a valid RPC definition to extract Request and Response message types from the .proto file."));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsTacticsSummary_WhenNoRpsImpactTacticsSelected() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(true, true),
                new TimeoutTactic(true, 500),
                new RetryTactic(false, 0),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",     true))
                .andExpect(model().attribute("rpsWasAdjusted", false))
                .andExpect(model().attribute("baseRps",        "1.000"))
                .andExpect(model().attribute("effectiveRps",   "1.000"))
                .andExpect(model().attribute("requestsPerMonth", "2.592.000.000"))
                .andExpect(model().attributeExists("infoTactics"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsTacticsSummary_WhenCircuitBreakerIsConfigured() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutTactic(false, 0),
                new RetryTactic(false, 0),
                new CircuitBreakerTactic(true, 10, 5, 60000, 50),
                new SAGAPattern(false, 0, 0, 0)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",     true))
                .andExpect(model().attribute("rpsWasAdjusted", false))
                .andExpect(model().attribute("requestsPerMonth", "2.592.000.000"))
                .andExpect(model().attributeExists("infoTactics"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenRetryIsConfigured() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutTactic(false, 0),
                new RetryTactic(true, 3),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",       true))
                .andExpect(model().attribute("rpsWasAdjusted",   true))
                .andExpect(model().attribute("baseRps",          "1.000"))
                .andExpect(model().attribute("effectiveRps",     "4.000"))
                .andExpect(model().attribute("requestsPerMonth", "10.368.000.000"))
                .andExpect(model().attributeExists("rpsTactics"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfigured() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutTactic(false, 0),
                new RetryTactic(false, 0),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(true, 1, 1, 1)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",       true))
                .andExpect(model().attribute("rpsWasAdjusted",   true))
                .andExpect(model().attribute("baseRps",          "1.000"))
                .andExpect(model().attribute("effectiveRps",     "3.000"))
                .andExpect(model().attribute("requestsPerMonth", "7.776.000.000"))
                .andExpect(model().attributeExists("rpsTactics"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfiguredWithMoreSteps() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutTactic(false, 0),
                new RetryTactic(false, 0),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(true, 2, 3, 1)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics", true))
                .andExpect(model().attribute("rpsWasAdjusted", true))
                .andExpect(model().attribute("baseRps", "1.000"))
                .andExpect(model().attribute("effectiveRps", "6.000"))
                .andExpect(model().attribute("requestsPerMonth", "15.552.000.000"))
                .andExpect(model().attributeExists("rpsTactics"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaAndRetryAreCombined() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutTactic(false, 0),
                new RetryTactic(true, 2),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(true, 1, 1, 1)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",       true))
                .andExpect(model().attribute("rpsWasAdjusted",   true))
                .andExpect(model().attribute("baseRps",          "1.000"))
                .andExpect(model().attribute("effectiveRps",     "5.000"))
                .andExpect(model().attribute("requestsPerMonth", "12.960.000.000"))
                .andExpect(model().attributeExists("infoTactics"))
                .andExpect(model().attributeExists("rpsTactics"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsTacticsSummary_WhenMixedTacticsSelected() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(true, false),
                new TimeoutTactic(true, 300),
                new RetryTactic(true, 3),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("hasTactics",       true))
                .andExpect(model().attribute("rpsWasAdjusted",   true))
                .andExpect(model().attribute("effectiveRps",     "4.000"))
                .andExpect(model().attribute("requestsPerMonth", "10.368.000.000"))
                .andExpect(model().attributeExists("infoTactics"))
                .andExpect(model().attributeExists("rpsTactics"));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfigured_OnLinux() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutTactic(false, 0),
                new RetryTactic(false, 0),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(true, 1, 1, 1)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("effectiveRps",     "3.000"))
                .andExpect(model().attribute("requestsPerMonth", "7.776.000.000"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfigured_OnWindows() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutTactic(false, 0),
                new RetryTactic(false, 0),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(true, 1, 1, 1)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("effectiveRps",     "3.000"))
                .andExpect(model().attribute("requestsPerMonth", "7.776.000.000"));
    }

    @Test
    @EnabledOnOs(OS.MAC)
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenSagaIsConfigured_OnMac() throws Exception {
        TacticsConfigDTO tactics = new TacticsConfigDTO(
                1000,
                new ReliabilityTactics(false, false),
                new TimeoutTactic(false, 0),
                new RetryTactic(false, 0),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(true, 1, 1, 1)
        );
        mockMvc.perform(multipart("/calculateTCO")
                        .file(buildProtoMultipartFile(VALID_PROTO_CONTENT.getBytes()))
                        .sessionAttr(SESSION_KEY, tactics))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attribute("effectiveRps",     "3.000"))
                .andExpect(model().attribute("requestsPerMonth", "7.776.000.000"));
    }

    private static TacticsConfigDTO noTactics(long requestsPerSecond) {
        return new TacticsConfigDTO(
                requestsPerSecond,
                new ReliabilityTactics(false, false),
                new TimeoutTactic(false, 0),
                new RetryTactic(false, 0),
                new CircuitBreakerTactic(false, 0, 0, 0, 0),
                new SAGAPattern(false, 0, 0, 0)
        );
    }

    private MockMultipartFile buildProtoMultipartFile(byte[] content) {
        return new MockMultipartFile("protoFile", "order.proto", "text/plain", content);
    }
}