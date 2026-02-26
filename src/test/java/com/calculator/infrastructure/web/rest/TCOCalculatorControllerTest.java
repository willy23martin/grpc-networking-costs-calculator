package com.calculator.infrastructure.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

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
                .andExpect(status().isOk())
                .andExpect(view().name("calculator"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_ShowsCosts_WhenProtofileIsValid() throws Exception {
        MockMultipartFile protoFile = new MockMultipartFile(
                "protoFile",
                "order.proto",
                "text/plain",
                """
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
                """.getBytes()
        );

        mockMvc.perform(multipart("/calculateTCO")
                        .file(protoFile)
                        .param("requestsPerSecond", "1000"))
                .andExpect(status().isOk())
                .andExpect(view().name("calculator"))
                .andExpect(model().attributeDoesNotExist("error"))
                .andExpect(model().attributeExists("requestSize"))
                .andExpect(model().attribute("requestSize", 30309))
                .andExpect(model().attributeExists("responseSize"))
                .andExpect(model().attribute("responseSize", 534))
                .andExpect(model().attributeExists("requestsPerMonth"))
                .andExpect(model().attribute("requestsPerMonth", "2.592.000.000"))
                .andExpect(model().attributeExists("requestGbPerMonth"))
                .andExpect(model().attribute("requestGbPerMonth", "73165,5657"))
                .andExpect(model().attributeExists("responseGbPerMonth"))
                .andExpect(model().attribute("responseGbPerMonth","1289,0697"))
                .andExpect(model().attributeExists("dataTransferCostUsd"))
                .andExpect(model().attribute("dataTransferCostUsd", "116,02"));
    }
}
