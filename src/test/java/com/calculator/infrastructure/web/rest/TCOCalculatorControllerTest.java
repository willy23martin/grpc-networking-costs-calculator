package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.networking.NetworkingCostCalculator;
import com.calculator.application.services.calculators.rps.RequestPerSecondCostCalculatorService;
import com.calculator.application.services.compilators.CompilationService;
import com.calculator.application.services.populator.TacticsPopulatorService;
import com.calculator.application.services.protobuf.ProtocolBufferMessageSizeCalculationService;
import com.calculator.application.services.protobuf.ProtocolBufferService;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.compilers.ProtocCompiler;
import com.calculator.domain.dto.properties.ProtoFileFullyQualifiedProperties;
import com.calculator.domain.dto.protofiles.JavaParsedProtoFile;
import com.calculator.domain.dto.protofiles.ParsedProtocolBufferFile;
import com.calculator.domain.dto.results.CompilationResult;
import com.calculator.domain.dto.results.MessageSizeCalculationResult;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.dto.tactics.security.oauth.jwt.JWTTactic;
import com.calculator.domain.dto.tactics.security.tls.TLSTactic;
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
import org.springframework.web.multipart.MultipartFile;

import javax.tools.JavaCompiler;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TCOCalculatorControllerUnitTest {

    @Mock private HttpSession session;

    @InjectMocks
    private TCOCalculatorController controller;

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
        // Initialize the static mock framework for utility methods
        mockedFileUtils = Mockito.mockStatic(ProtocolBufferParsedFileUtils.class);
    }

    @AfterEach
    void tearDown() {
        // Safe closure to prevent test memory contamination
        if (mockedFileUtils != null) {
            mockedFileUtils.close();
        }
    }

    @Test
    void init_PopulatesEmptyBytesAndReturnsCalculatorView() {
        String view = controller.init(model);

        assertEquals("calculator", view);
        assertEquals("", model.getAttribute("requestMessageBytes"));
        assertEquals("", model.getAttribute("responseMessageBytes"));
    }

    @Test
    void calculateProtoFileTCONetworkingCosts_NullOrEmptyFile_ReturnsEarlyWithSelectionMessage() {
        String viewNull = controller.calculateProtoFileTCONetworkingCosts(null, session, model);
        assertEquals("calculator", viewNull);
        assertEquals("No file selected for upload.", model.getAttribute("uploadMessage"));

        MockMultipartFile emptyFile = new MockMultipartFile("protoFile", "", "text/plain", new byte[0]);
        String viewEmpty = controller.calculateProtoFileTCONetworkingCosts(emptyFile, session, model);
        assertEquals("calculator", viewEmpty);
    }

}