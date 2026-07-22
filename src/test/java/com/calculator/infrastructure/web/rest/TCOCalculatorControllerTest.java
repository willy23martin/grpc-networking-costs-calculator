package com.calculator.infrastructure.web.rest;

import com.calculator.shared.ProtocolBufferParsedFileUtils;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NetworkingCostCalculatorControllerTest {

    @Mock
    private HttpSession session;

    @InjectMocks
    private NetworkingCostCalculatorController controller;

    private Model model;
    MockMultipartFile validProtoFile;
    private MockedStatic<ProtocolBufferParsedFileUtils> mockedFileUtils;

    @BeforeEach
    void setUp() {
        model = new ExtendedModelMap();
        validProtoFile = new MockMultipartFile(
                "protoFile",
                "service.proto",
                "text/plain",
                "syntax = \"proto3\"; option java_package = \"com.test\"; message Req {} message Resp {}".getBytes()
        );
        mockedFileUtils = Mockito.mockStatic(ProtocolBufferParsedFileUtils.class);
    }

    @AfterEach
    void tearDown() {
        if (mockedFileUtils != null) {
            mockedFileUtils.close();
        }
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_NullOrEmptyFile_ReturnsEarlyWithSelectionMessage() {
        String viewNull = controller.calculateProtoFileNetworkingCosts(null, session, model);
        assertEquals("calculator", viewNull);
        assertEquals("No file selected for upload.", model.getAttribute("uploadMessage"));

        MockMultipartFile emptyFile = new MockMultipartFile("protoFile", "", "text/plain", new byte[0]);
        String viewEmpty = controller.calculateProtoFileNetworkingCosts(emptyFile, session, model);
        assertEquals("calculator", viewEmpty);
    }

}