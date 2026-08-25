package com.calculator.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolBuffersUtilsTest {

    public static final String VALID_PROTO_CONTENT = """
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

    public static final String VALID_PROTO_CONTENT_BUC2 = """
            syntax = "proto3";
            
            package com.ecommerce.order.search;
            
            option java_package = "com.ecommerce.order.grpc.search";
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
            
            message SearchOrdersRequest {
              string order_id = 1;
              OrderStatus status_filter = 2;
            }
            
            message OrderItem {
              string product_id = 1;
              string product_name = 2;
              int32 quantity = 3;
              double unit_price = 4;
              double subtotal = 5;
            }
            
            message ShippingAddress {
              string street = 1;
              string city = 2;
              string state = 3;
              string postal_code = 4;
              string country = 5;
            }
            
            message OrderSearchResult {
              string order_id = 1;
              string customer_id = 2;
              string customer_name = 3;
              OrderStatus status = 4;
              google.protobuf.Timestamp created_at = 5;
              google.protobuf.Timestamp updated_at = 6;
              double total_amount = 7;
              string currency = 8;
              repeated OrderItem items = 9;
              ShippingAddress shipping_address = 10;
            }
            
            service OrderSearchService {
              rpc SearchOrders (SearchOrdersRequest) returns (stream OrderSearchResult);
            }
            """;

    public static final String VALID_PROTO_CONTENT_BUC3 = """
            syntax = "proto3";
            
            package com.ecommerce.order.batchupdate;
            
            option java_package = "com.ecommerce.order.grpc.batchupdate";
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
            
            enum UpdateAction {
              UPDATE_ACTION_UNSPECIFIED = 0;
              UPDATE_ACTION_STATUS_CHANGE = 1;
              UPDATE_ACTION_ADD_NOTE = 2;
              UPDATE_ACTION_UPDATE_SHIPPING = 3;
              UPDATE_ACTION_MODIFY_AMOUNT = 4;
              UPDATE_ACTION_CANCEL = 5;
            }
            
            message ShippingAddress {
              string street = 1;
              string city = 2;
              string state = 3;
              string postal_code = 4;
              string country = 5;
            }
            
            message ShippingUpdate {
              string tracking_number = 1;
              string carrier = 2;
              google.protobuf.Timestamp estimated_delivery = 3;
              ShippingAddress new_address = 4;
            }
            
            message OrderUpdateRequest {
              string order_id = 1;
              UpdateAction action = 2;
              OrderStatus new_status = 3;
              string status_reason = 4;
              string note = 5;
              ShippingUpdate shipping_update = 6;
              double new_amount = 7;
              string amount_adjustment_reason = 8;
              string updated_by = 9;
              google.protobuf.Timestamp request_time = 10;
            }
            
            message UpdateResult {
              string order_id = 1;
              bool success = 2;
              string error_message = 3;
              OrderStatus previous_status = 4;
              OrderStatus current_status = 5;
              google.protobuf.Timestamp updated_at = 6;
            }
            
            message BatchUpdateResponse {
              int32 total_received = 1;
              int32 successful_updates = 2;
              int32 failed_updates = 3;
              repeated UpdateResult results = 4;
              google.protobuf.Timestamp processing_started = 5;
              google.protobuf.Timestamp processing_completed = 6;
              double processing_duration_seconds = 7;
            }
            
            service OrderBatchUpdateService {
              rpc BatchUpdateOrders (stream OrderUpdateRequest) returns (BatchUpdateResponse);
            }
            """;

    public static final String VALID_PROTO_CONTENT_BUC4 = """
            syntax = "proto3";
            
            package com.ecommerce.order.bidirectional;
            
            option java_package = "com.ecommerce.order.grpc.bidirectional";
            option java_multiple_files = true;
            
            import "google/protobuf/timestamp.proto";
            
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
            
            message OrderProcessingRequest {
              string order_id = 1;
              string user_id = 2;
              repeated OrderItem items = 3;
              Address shipping_address = 4;
              google.protobuf.Timestamp requested_delivery_date = 5;
            }
            
            message CombinedShipment {
              string shipment_id = 1;
              repeated string order_ids = 2;
              google.protobuf.Timestamp delivery_date = 3;
              Address shipping_address = 4;
              Money total_shipping_cost = 5;
              int32 total_items = 6;
              google.protobuf.Timestamp created_at = 7;
            }
            
            service BidirectionalOrderProcessingService {
              rpc ProcessOrdersForCombinedShipments (stream OrderProcessingRequest) returns (stream CombinedShipment);
            }
            """;

    @Test
    void importGoogleProtocolBufferFileDependencies_allFilesCreated(@TempDir Path tempDir) throws IOException {
        final Path result = ProtocolBuffersUtils.importGoogleProtocolBufferFileDependencies(tempDir);

        final Path googleDir = tempDir.resolve("google/protobuf");
        assertAll("All well-known types copied",
                () -> assertTrue(Files.exists(googleDir)),
                () -> assertTrue(Files.exists(googleDir.resolve("timestamp.proto"))),
                () -> assertTrue(Files.exists(googleDir.resolve("duration.proto"))),
                () -> assertTrue(Files.exists(googleDir.resolve("wrappers.proto"))),
                () -> assertTrue(Files.exists(googleDir.resolve("any.proto")))
        );
        assertEquals(tempDir, result);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void resolveProtocolBufferFilesCompiler_windowsPath() {
        System.setProperty("os.name", "Windows 10");
        System.setProperty("os.arch", "x86_64");
        System.setProperty("user.home", "/home/test");

        final Path result = ProtocolBuffersUtils.resolveProtocolBufferFilesCompiler();

        final String expected = "/home/test/.m2/repository/com/google/protobuf/protoc/4.29.4/protoc-4.29.4-windows-x86_64.exe";
        assertEquals(Path.of(expected), result);
    }

    @EnabledOnOs(OS.MAC)
    @Test
    void resolveProtocolBufferFilesCompiler_macAarch64_executable() {
        System.setProperty("os.name", "Mac OS X");
        System.setProperty("os.arch", "aarch64");
        System.setProperty("user.home", "/Users/test");

        final Path result = ProtocolBuffersUtils.resolveProtocolBufferFilesCompiler();

        assertTrue(result.toString().contains("osx-aarch_64.exe"));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void resolveProtocolBufferFilesCompiler_linuxArm64_fallsToAarch64() {
        System.setProperty("os.name", "Linux");
        System.setProperty("os.arch", "arm64");
        System.setProperty("user.home", "/home/test");

        final Path result = ProtocolBuffersUtils.resolveProtocolBufferFilesCompiler();

        assertTrue(result.toString().contains("linux-aarch_64.exe"));
    }

}