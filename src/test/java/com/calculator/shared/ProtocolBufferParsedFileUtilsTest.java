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
        final JavaParsedProtoFile validProto = new JavaParsedProtoFile(
                "com.example", "OuterClass", "MyRequest", "MyResponse");

        assertTrue(ProtocolBufferParsedFileUtils.isValid(validProto));
    }

    @Test
    void isValid_emptyRequestMessage_returnsFalse() {
        final JavaParsedProtoFile invalidProto = new JavaParsedProtoFile(
                "com.example", "OuterClass", "", "MyResponse");

        assertFalse(ProtocolBufferParsedFileUtils.isValid(invalidProto));
    }

    @Test
    void isValid_emptyResponseMessage_returnsFalse() {
        final JavaParsedProtoFile invalidProto = new JavaParsedProtoFile(
                "com.example", "OuterClass", "MyRequest", "");

        assertFalse(ProtocolBufferParsedFileUtils.isValid(invalidProto));
    }

    @Test
    void initializeProtoFileFullyQualifiedProperties_allFieldsPresent_correctFullNames() {
        final JavaParsedProtoFile protoFile = new JavaParsedProtoFile(
                "com.example", "RequestProto", "MyRequest", "MyResponse");

        final ProtoFileFullyQualifiedProperties result = ProtocolBufferParsedFileUtils
                .initializeProtoFileFullyQualifiedProperties(protoFile);

        assertEquals("com.example.RequestProto$MyRequest", result.fullRequestMessageClassName());
        assertEquals("com.example.RequestProto$MyResponse", result.fullResponseMessageClassName());
    }

    @ParameterizedTest(name = "package={0}, outer={1}, request={2}, response={3}")
    @MethodSource("allCombinationsProvider")
    void initializeProtoFileFullyQualifiedProperties_allCombinations_correctFormat(
            String packageName, String outerClass, String requestSimple, String responseSimple,
            String expectedRequest, String expectedResponse) {

        final JavaParsedProtoFile protoFile = new JavaParsedProtoFile(packageName, outerClass, requestSimple, responseSimple);

        final ProtoFileFullyQualifiedProperties result = ProtocolBufferParsedFileUtils
                .initializeProtoFileFullyQualifiedProperties(protoFile);

        assertEquals(expectedRequest, result.fullRequestMessageClassName());
        assertEquals(expectedResponse, result.fullResponseMessageClassName());
    }

    static Stream<Arguments> allCombinationsProvider() {
        return Stream.of(
                Arguments.of("com.example", "OuterClass", "Req", "Resp",
                        "com.example.OuterClass$Req", "com.example.OuterClass$Resp"),

                Arguments.of("com.example", "", "Req", "Resp",
                        "com.example.Req", "com.example.Resp"),

                Arguments.of("", "OuterClass", "Req", "Resp",
                        "OuterClass$Req", "OuterClass$Resp"),

                Arguments.of("", "", "Req", "Resp",
                        "Req", "Resp")
        );
    }

    @Test
    void isValid_validatesBeforeInitialize() {
        final JavaParsedProtoFile invalidProto = new JavaParsedProtoFile("", "", "", "Response");

        final ProtoFileFullyQualifiedProperties result = ProtocolBufferParsedFileUtils
                .initializeProtoFileFullyQualifiedProperties(invalidProto);

        assertFalse(ProtocolBufferParsedFileUtils.isValid(invalidProto));
        assertEquals("Response", result.fullResponseMessageClassName());
    }
}