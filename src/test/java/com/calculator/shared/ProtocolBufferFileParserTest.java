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
            
            option java_package = "com.example.myapp";
            option java_outer_classname = "MyProtoService";
            
            service MyService {
              rpc SayHello (HelloRequest) returns (HelloResponse) {}
            }
            
            message HelloRequest {
              string name = 1;
            }
            
            message HelloResponse {
              string message = 1;
            }
            """;

        Path protoFile = createProtoFile(tempDir, "test.proto", protoContent);

        JavaParsedProtoFile result = ProtocolBufferFileParser.parseProtoFileFrom(protoFile);

        assertEquals("com.example.myapp", result.javaPackageName());
        assertEquals("MyProtoService", result.outerClassName());
        assertEquals("HelloRequest", result.requestMessageSimpleName());
        assertEquals("HelloResponse", result.responseMessageSimpleName());
    }

    @Test
    void parseProtoFileFrom_onlyPackageAndRpc_extractsAvailableFields(@TempDir Path tempDir) throws IOException {
        String protoContent = """
            syntax = "proto3";
            
            option java_package = "com.example";
            
            service TestService {
              rpc GetData (DataRequest) returns (DataResponse) {}
            }
            """;

        Path protoFile = createProtoFile(tempDir, "test.proto", protoContent);

        JavaParsedProtoFile result = ProtocolBufferFileParser.parseProtoFileFrom(protoFile);

        assertEquals("com.example", result.javaPackageName());
        assertEquals("", result.outerClassName());
        assertEquals("DataRequest", result.requestMessageSimpleName());
        assertEquals("DataResponse", result.responseMessageSimpleName());
    }

    @Test
    void parseProtoFileFrom_noJavaOptions_onlyRpc(@TempDir Path tempDir) throws IOException {
        String protoContent = """
            syntax = "proto3";
            
            service SimpleService {
              rpc Ping (PingRequest) returns (PingResponse) {}
            }
            """;

        Path protoFile = createProtoFile(tempDir, "test.proto", protoContent);

        JavaParsedProtoFile result = ProtocolBufferFileParser.parseProtoFileFrom(protoFile);

        assertEquals("", result.javaPackageName());
        assertEquals("", result.outerClassName());
        assertEquals("PingRequest", result.requestMessageSimpleName());
        assertEquals("PingResponse", result.responseMessageSimpleName());
    }

    @Test
    void parseProtoFileFrom_multipleRpc_firstOnly(@TempDir Path tempDir) throws IOException {
        String multipleRPCsShouldTakeFirstOnlyIsEmptyCheck = """
            syntax = "proto3";
            
            service MultiService {
              rpc FirstCall (FirstRequest) returns (FirstResponse) {}
              rpc SecondCall (SecondRequest) returns (SecondResponse) {}
            }
            """;

        Path protoFile = createProtoFile(tempDir, "test.proto", multipleRPCsShouldTakeFirstOnlyIsEmptyCheck);

        JavaParsedProtoFile result = ProtocolBufferFileParser.parseProtoFileFrom(protoFile);

        assertEquals("FirstRequest", result.requestMessageSimpleName());
        assertEquals("FirstResponse", result.responseMessageSimpleName());
    }

    @Test
    void parseProtoFileFrom_emptyFile_emptyResult(@TempDir Path tempDir) throws IOException {
        Path emptyProtoFile = createProtoFile(tempDir, "empty.proto", "");

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