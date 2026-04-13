package com.calculator.shared;

import com.calculator.domain.dto.protofiles.JavaParsedProtoFile;
import com.calculator.domain.dto.properties.ProtoFileFullyQualifiedProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolBufferParsedFileUtilsTest {

    @Test
    void isValid_bothMessagesPresent_returnsTrue() {
        JavaParsedProtoFile validProto = new JavaParsedProtoFile(
                "com.example", "OuterClass", "MyRequest", "MyResponse");

        assertTrue(ProtocolBufferParsedFileUtils.isValid(validProto));
    }

    @Test
    void isValid_emptyRequestMessage_returnsFalse() {
        JavaParsedProtoFile invalidProto = new JavaParsedProtoFile(
                "com.example", "OuterClass", "", "MyResponse");

        assertFalse(ProtocolBufferParsedFileUtils.isValid(invalidProto));
    }

    @Test
    void isValid_emptyResponseMessage_returnsFalse() {
        JavaParsedProtoFile invalidProto = new JavaParsedProtoFile(
                "com.example", "OuterClass", "MyRequest", "");

        assertFalse(ProtocolBufferParsedFileUtils.isValid(invalidProto));
    }

    @Test
    void initializeProtoFileFullyQualifiedProperties_allFieldsPresent_correctFullNames() {
        JavaParsedProtoFile protoFile = new JavaParsedProtoFile(
                "com.example", "RequestProto", "MyRequest", "MyResponse");

        ProtoFileFullyQualifiedProperties result = ProtocolBufferParsedFileUtils
                .initializeProtoFileFullyQualifiedProperties(protoFile);

        assertEquals("com.example.RequestProto$MyRequest", result.fullRequestMessageClassName());
        assertEquals("com.example.RequestProto$MyResponse", result.fullResponseMessageClassName());
    }

    @ParameterizedTest(name = "package={0}, outer={1}, request={2}, response={3}")
    @MethodSource("allCombinationsProvider")
    void initializeProtoFileFullyQualifiedProperties_allCombinations_correctFormat(
            String packageName, String outerClass, String requestSimple, String responseSimple,
            String expectedRequest, String expectedResponse) {

        JavaParsedProtoFile protoFile = new JavaParsedProtoFile(packageName, outerClass, requestSimple, responseSimple);

        ProtoFileFullyQualifiedProperties result = ProtocolBufferParsedFileUtils
                .initializeProtoFileFullyQualifiedProperties(protoFile);

        assertEquals(expectedRequest, result.fullRequestMessageClassName());
        assertEquals(expectedResponse, result.fullResponseMessageClassName());
    }

    static Stream<Arguments> allCombinationsProvider() {
        return Stream.of(
                // Because it's package + outer + simple (both request/response)
                Arguments.of("com.example", "OuterClass", "Req", "Resp",
                        "com.example.OuterClass$Req", "com.example.OuterClass$Resp"),

                // Because it's package + simple (no outer)
                Arguments.of("com.example", "", "Req", "Resp",
                        "com.example.Req", "com.example.Resp"),

                // Because it's outer + simple (no package)
                Arguments.of("", "OuterClass", "Req", "Resp",
                        "OuterClass$Req", "OuterClass$Resp"),

                // Because it's simple only (no package, no outer)
                Arguments.of("", "", "Req", "Resp",
                        "Req", "Resp")
        );
    }

    @Test
    void isValid_validatesBeforeInitialize() {
        JavaParsedProtoFile invalidProto = new JavaParsedProtoFile("", "", "", "Response");

        assertFalse(ProtocolBufferParsedFileUtils.isValid(invalidProto));
        ProtoFileFullyQualifiedProperties result = ProtocolBufferParsedFileUtils
                .initializeProtoFileFullyQualifiedProperties(invalidProto);
        assertEquals("Response", result.fullResponseMessageClassName());
    }
}