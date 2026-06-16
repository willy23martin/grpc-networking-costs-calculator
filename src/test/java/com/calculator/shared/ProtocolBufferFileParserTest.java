package com.calculator.shared;

import com.calculator.domain.dto.protofiles.JavaParsedProtoFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolBufferFileParserTest {

    @Test
    void parseProtoFileFrom_completeProtoFile_allFieldsParsed(@TempDir Path tempDir) throws IOException {
        String protoContent = """
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
                }
                
                message GetOrderRequest {
                  string order_id = 1;
                }
                
                service UnaryOrderService {
                  rpc GetOrderDetails (GetOrderRequest) returns (Order);
                }
            """;

        Path protoFile = createProtoFile(tempDir, "buc1_unary_order.proto", protoContent);

        JavaParsedProtoFile result = ProtocolBufferFileParser.parseProtoFileFrom(protoFile);

        assertEquals("com.ecommerce.order.grpc.unary", result.javaPackageName());
        assertEquals("", result.outerClassName());
        assertEquals("GetOrderRequest", result.requestMessageSimpleName());
        assertEquals("Order", result.responseMessageSimpleName());
    }

    @Test
    void parseProtoFileFrom_onlyPackageAndRpc_extractsAvailableFields(@TempDir Path tempDir) throws IOException {
        String protoContent = """
            syntax = "proto3";
            
            option java_package = "com.ecommerce.order.grpc.unary";
            
            service UnaryOrderService {
              rpc GetOrderDetails (GetOrderRequest) returns (Order) {}
            }
            """;

        Path protoFile = createProtoFile(tempDir, "minimal_unary.proto", protoContent);

        JavaParsedProtoFile result = ProtocolBufferFileParser.parseProtoFileFrom(protoFile);

        assertEquals("com.ecommerce.order.grpc.unary", result.javaPackageName());
        assertEquals("", result.outerClassName());
        assertEquals("GetOrderRequest", result.requestMessageSimpleName());
        assertEquals("Order", result.responseMessageSimpleName());
    }

    @Test
    void parseProtoFileFrom_noJavaOptions_onlyRpc(@TempDir Path tempDir) throws IOException {
        String protoContent = """
            syntax = "proto3";
            
            service UnaryOrderService {
              rpc GetOrderDetails (GetOrderRequest) returns (Order) {}
            }
            """;

        Path protoFile = createProtoFile(tempDir, "no_options_unary.proto", protoContent);

        JavaParsedProtoFile result = ProtocolBufferFileParser.parseProtoFileFrom(protoFile);

        assertEquals("", result.javaPackageName());
        assertEquals("", result.outerClassName());
        assertEquals("GetOrderRequest", result.requestMessageSimpleName());
        assertEquals("Order", result.responseMessageSimpleName());
    }

    @Test
    void parseProtoFileFrom_multipleRpc_firstOnly(@TempDir Path tempDir) throws IOException {
        String multipleRPCsContent = """
            syntax = "proto3";
            
            service UnaryOrderService {
              rpc GetOrderDetails (GetOrderRequest) returns (Order) {}
              rpc FallbackVerification (GetOrderRequest) returns (Order) {}
            }
            """;

        Path protoFile = createProtoFile(tempDir, "multi_rpc_unary.proto", multipleRPCsContent);

        JavaParsedProtoFile result = ProtocolBufferFileParser.parseProtoFileFrom(protoFile);

        // Ensures the parser selects the primary baseline RPC execution contract (first matching metadata match)
        assertEquals("GetOrderRequest", result.requestMessageSimpleName());
        assertEquals("Order", result.responseMessageSimpleName());
    }

    @Test
    void parseProtoFileFrom_emptyFile_emptyResult(@TempDir Path tempDir) throws IOException {
        Path emptyProtoFile = createProtoFile(tempDir, "empty_malformed.proto", "");

        JavaParsedProtoFile result = ProtocolBufferFileParser.parseProtoFileFrom(emptyProtoFile);

        assertEquals("", result.javaPackageName());
        assertEquals("", result.outerClassName());
        assertEquals("", result.requestMessageSimpleName());
        assertEquals("", result.responseMessageSimpleName());
    }

    private Path createProtoFile(Path tempDir, String filename, String content) throws IOException {
        Path protoFile = tempDir.resolve(filename);
        Files.write(protoFile, content.getBytes());
        return protoFile;
    }
}