package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.networking.NetworkingCostCalculator;
import com.calculator.application.services.calculators.rps.RequestPerSecondCostCalculatorService;
import com.calculator.application.services.compilators.CompilationService;
import com.calculator.application.services.populator.TacticsPopulatorService;
import com.calculator.application.services.protobuf.ProtocolBufferMessageSizeCalculationService;
import com.calculator.application.services.protobuf.ProtocolBufferService;
import com.calculator.domain.dto.compilers.ProtocCompiler;
import com.calculator.domain.dto.properties.ProtoFileFullyQualifiedProperties;
import com.calculator.domain.dto.protofiles.ParsedProtocolBufferFile;
import com.calculator.domain.dto.results.CompilationResult;
import com.calculator.domain.dto.results.JavaCompilationResult;
import com.calculator.domain.dto.results.MessageSizeCalculationResult;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.dto.tactics.security.tls.TLSOverhead;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.CompactNumberFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.calculator.domain.model.architecture.tactics.security.JWTOverhead.*;
import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static com.calculator.shared.ProtocolBufferParsedFileUtils.initializeProtoFileFullyQualifiedProperties;
import static com.calculator.shared.ProtocolBufferParsedFileUtils.isValid;

@Controller
public class TCOCalculatorController implements ErrorController {

    public static final double SECONDS_PER_MONTH = 2_592_000.0;
    public static final double BYTES_PER_GB      = 1_073_741_824.0;
    public static final Locale DISPLAY_LOCALE    = Locale.forLanguageTag("en-US");

    private static final String NO_TACTICS_CONFIGURATION_FOUND_MESSAGE =
            """
            Note: No tactic configuration found in your session.
            Results are based on default settings (all tactics disabled).
            Configure tactics and re-upload to get accurate results.
            """;

    @Autowired
    NetworkingCostCalculator networkingCostCalculator;

    @Autowired
    ProtocolBufferService protocolBufferService;

    @Autowired
    CompilationService compilationService;

    @Autowired
    ProtocolBufferMessageSizeCalculationService protocolBufferMessageSizeCalculationService;

    @Autowired
    RequestPerSecondCostCalculatorService requestsPerSecondCalculatorService;

    @Autowired
    TacticsPopulatorService tacticsModelPopulatorService;

    private final Logger log = Logger.getLogger(TCOCalculatorController.class.getName());

    @RequestMapping("/")
    public String init(Model model) {
        model.addAttribute("requestMessageBytes", "");
        model.addAttribute("responseMessageBytes", "");
        return "calculator";
    }

    @PostMapping("/calculateTCO")
    public String calculateProtoFileTCONetworkingCosts(
            @RequestParam("protoFile") MultipartFile protoFile,
            HttpSession session,
            Model model) {

        if (protoFile == null || protoFile.isEmpty()) {
            model.addAttribute("uploadMessage", "No file selected for upload.");
            return "calculator";
        }

        ArchitecturalDecisionsDTO architecturalDecisionsDTO = (ArchitecturalDecisionsDTO) session.getAttribute(SESSION_KEY);
        if (architecturalDecisionsDTO == null) {
            architecturalDecisionsDTO = ArchitecturalDecisionsDTO.empty();
            model.addAttribute("uploadMessage", NO_TACTICS_CONFIGURATION_FOUND_MESSAGE);
        }

        String view = calculateTCO(protoFile, architecturalDecisionsDTO, model);
        return (view != null) ? view : "calculator";
    }

    private String calculateTCO(MultipartFile protoFile, ArchitecturalDecisionsDTO architecturalDecisionsDTO, Model model) {
        try {
            updateBytesSizeWithProtoFileSize(protoFile, model);
            Path protocolBufferFileDirectory = null;
            try {
                ParsedProtocolBufferFile parsed = protocolBufferService.parse(protoFile);
                protocolBufferFileDirectory = parsed.protocolBufferFileDirectory();

                if (!isValid(parsed.javaParsedProtoFile())) {
                    model.addAttribute("error",
                            "Could not find a valid RPC definition to extract Request and Response message types from the .proto file.");
                    return "calculator";
                }

                ProtoFileFullyQualifiedProperties protoProps =
                        initializeProtoFileFullyQualifiedProperties(parsed.javaParsedProtoFile());

                ProtocCompiler protoCompiler = executeProtoc(model, parsed);
                if (protoCompiler == null) return "calculator";

                JavaCompilationResult javaCompilationResult =
                        compile(model, protoCompiler, protoProps.fullRequestMessageClassName());
                if (javaCompilationResult.errorCompilationMessage() != null)
                    return javaCompilationResult.errorCompilationMessage();

                URLClassLoader classLoader = javaCompilationResult.compilationResult().classLoader();

                long effectiveRequestsPerSecond = requestsPerSecondCalculatorService.calculateEffectiveRequestsPerSecond(architecturalDecisionsDTO);
                populateTactics(architecturalDecisionsDTO, effectiveRequestsPerSecond, model);
                showTCOCosts(effectiveRequestsPerSecond, model, protoProps, classLoader, architecturalDecisionsDTO);

            } catch (Exception e) {
                model.addAttribute("error", "Error: " + e.getMessage());
                log.warning(e.getMessage());
            } finally {
                String cleanUpError = cleanUp(model, protocolBufferFileDirectory);
                log.warning(cleanUpError);
            }
        } catch (Exception e) {
            model.addAttribute("uploadMessage", "File upload failed: " + e.getMessage());
            model.addAttribute("requestMessageBytes", "File upload failed: ");
            model.addAttribute("responseMessageBytes", "File upload failed: ");
            log.warning(e.getMessage());
        }
        return null;
    }

    private void populateTactics(ArchitecturalDecisionsDTO architecturalDecisionsDTO, long effectiveRequestsPerSecond, Model model) {
        List<Map<String, String>> infoTactics = new ArrayList<>();
        List<Map<String, String>> rpsTactics  = new ArrayList<>();
        tacticsModelPopulatorService.populateTacticsModel(architecturalDecisionsDTO, rpsTactics, infoTactics);
        updateViewInfoAffectedByTactics(architecturalDecisionsDTO.requestsPerSecond(), effectiveRequestsPerSecond, model, infoTactics, rpsTactics);
    }

    private static void updateViewInfoAffectedByTactics(long requestsPerSecond, long effectiveRps, Model model, List<Map<String, String>> infoTactics, List<Map<String, String>> rpsTactics) {
        model.addAttribute("infoTactics", infoTactics);
        model.addAttribute("rpsTactics", rpsTactics);
        model.addAttribute("hasTactics",     !infoTactics.isEmpty() || !rpsTactics.isEmpty());
        model.addAttribute("baseRps",        String.format(DISPLAY_LOCALE, "%,d", requestsPerSecond));
        model.addAttribute("effectiveRps",   String.format(DISPLAY_LOCALE, "%,d", effectiveRps));
        model.addAttribute("rpsWasAdjusted", effectiveRps != requestsPerSecond);
    }

    private void showTCOCosts(long effectiveRps,
                              Model model, ProtoFileFullyQualifiedProperties protoProps, URLClassLoader classLoader, ArchitecturalDecisionsDTO architecturalDecisionsDTO) {

        MessageSizeCalculationResult requestResult = calculateRequestMessageSize(model, classLoader, protoProps.fullRequestMessageClassName());
        MessageSizeCalculationResult responseResult = calculateResponseMessageSize(model, classLoader, protoProps.fullResponseMessageClassName());

        int tlsOverhead = architecturalDecisionsDTO.securityTactics().tlsTactic().effectiveTlsOverheadTypical();
        int jwtOverhead = architecturalDecisionsDTO.securityTactics().jwtTactic().effectiveJwtOverheadTypical();

        long effectiveRequestSize = requestResult.size() + tlsOverhead + jwtOverhead;
        long effectiveResponseSize = responseResult.size() + tlsOverhead;

        long requestsPerMonth = (long) (effectiveRps * SECONDS_PER_MONTH);
        double requestGbPerMonth = (effectiveRequestSize  * effectiveRps * SECONDS_PER_MONTH) / BYTES_PER_GB;
        double responseGbPerMonth = (effectiveResponseSize * effectiveRps * SECONDS_PER_MONTH) / BYTES_PER_GB;
        double dataTransferCostUsd = networkingCostCalculator.calculateDataTransferCost(responseGbPerMonth);

        NumberFormat compactNumberFormat = CompactNumberFormat.getCompactNumberInstance(
                Locale.US, NumberFormat.Style.SHORT
        );

        model.addAttribute("requestsPerMonth",    compactNumberFormat.format(requestsPerMonth));
        model.addAttribute("requestGbPerMonth",   String.format(DISPLAY_LOCALE, "%,.4f",  requestGbPerMonth));
        model.addAttribute("responseGbPerMonth",  String.format(DISPLAY_LOCALE, "%,.4f",  responseGbPerMonth));
        model.addAttribute("dataTransferCostUsd", String.format(DISPLAY_LOCALE, "%,.2f", dataTransferCostUsd));

        showEffectiveSizesAndSecurityOverheadDetailForResultsTable(model, architecturalDecisionsDTO.securityTactics(), requestResult, responseResult, effectiveRequestSize, effectiveResponseSize, tlsOverhead, jwtOverhead);
    }

    private static void showEffectiveSizesAndSecurityOverheadDetailForResultsTable(Model model, SecurityTactics security, MessageSizeCalculationResult requestResult, MessageSizeCalculationResult responseResult, long effectiveRequestSize, long effectiveResponseSize, int tlsOverhead, int jwtOverhead) {
        model.addAttribute("requestSize",        requestResult.size());
        model.addAttribute("responseSize",        responseResult.size());
        model.addAttribute("requestSizeEffective", effectiveRequestSize);
        model.addAttribute("responseSizeEffective", effectiveResponseSize);
        model.addAttribute("securityByteOverheadApplied",
                tlsOverhead > 0 || jwtOverhead > 0);
        model.addAttribute("tlsOverheadBytes", tlsOverhead);
        model.addAttribute("jwtOverheadBytes", jwtOverhead);
        model.addAttribute("tlsOverheadRange",
                security.tlsTactic().tlsEnabled() || security.tlsTactic().mtlsEnabled()
                        ? TLSOverhead.RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX.getOverhead() + " / " +
                         " bytes typical" : null);
        model.addAttribute("jwtOverheadRange",
                security.jwtTactic().oauthJwtEnabled()
                        ? JWT_OVERHEAD_BYTES_MIN.getOverhead() + " / " +
                        JWT_OVERHEAD_BYTES_TYPICAL.getOverhead() + " / " +
                        JWT_OVERHEAD_BYTES_MAX.getOverhead() + " bytes (min/typical/max)"
                        : null);
    }

    private ProtocCompiler executeProtoc(Model model, ParsedProtocolBufferFile parsed)
            throws IOException, InterruptedException {
        ProtocCompiler protoCompiler = protocolBufferService.runProtocOver(parsed);
        int exit = protoCompiler.protoc().waitFor();
        if (exit != 0) {
            String error = new String(protoCompiler.protoc().getInputStream().readAllBytes());
            model.addAttribute("error", "protoc failed: " + error);
            return null;
        }
        return protoCompiler;
    }

    private JavaCompilationResult compile(Model model, ProtocCompiler protoCompiler,
                                          String fullClassNameForRequestMessage)
            throws IOException, ClassNotFoundException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            model.addAttribute("error", "No Java compiler available in this environment.");
            return new JavaCompilationResult("calculator", null);
        }
        CompilationResult compilationResult =
                compilationService.compile(protoCompiler, compiler, fullClassNameForRequestMessage);
        if (compilationResult.compilationResult() != 0) {
            model.addAttribute("error", "Compilation of generated Java code failed.");
            return new JavaCompilationResult("calculator", compilationResult);
        }
        return new JavaCompilationResult(null, compilationResult);
    }

    private MessageSizeCalculationResult calculateRequestMessageSize(
            Model model, URLClassLoader classLoader, String fullClassName) {
        MessageSizeCalculationResult result = new MessageSizeCalculationResult(null, 0);
        try {
            result = protocolBufferMessageSizeCalculationService.getMessageSize(classLoader, fullClassName);
            model.addAttribute("requestSize", result.size());
        } catch (Exception e) {
            log.warning(e.getMessage());
            model.addAttribute("requestSize", 0);
            model.addAttribute("requestMessageError", "Failed to calculate max request size: " + e.getMessage());
        }
        return result;
    }

    private MessageSizeCalculationResult calculateResponseMessageSize(
            Model model, URLClassLoader classLoader, String fullClassName) {
        MessageSizeCalculationResult result = new MessageSizeCalculationResult(null, 0);
        try {
            result = protocolBufferMessageSizeCalculationService.getMessageSize(classLoader, fullClassName);
            model.addAttribute("responseSize", result.size());
        } catch (ClassNotFoundException e) {
            log.warning(e.getMessage());
            model.addAttribute("responseSize", 0);
            model.addAttribute("responseMessageError",
                    "Could not load response message: '" + fullClassName + "'. " + e.getMessage());
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.warning(e.getMessage());
            model.addAttribute("responseSize", 0);
            model.addAttribute("responseMessageError", "Error processing response message: " + e.getMessage());
        } catch (Exception e) {
            log.warning(e.getMessage());
            model.addAttribute("responseSize", 0);
            model.addAttribute("responseMessageError",
                    "An unexpected error occurred during response calculation: " + e.getMessage());
        }
        return result;
    }

    private static void updateBytesSizeWithProtoFileSize(MultipartFile protoFile, Model model) {
        model.addAttribute("uploadMessage",
                "File '" + protoFile.getOriginalFilename() + "' uploaded successfully!");
        model.addAttribute("requestMessageBytes",
                "Request Message bytes: " + protoFile.getSize() + " calculated successfully.");
        model.addAttribute("responseMessageBytes",
                "Response Message bytes: " + protoFile.getSize() + " calculated successfully.");
    }

    private static String cleanUp(Model model, Path protocolBufferFileDirectory) {
        if (protocolBufferFileDirectory != null) {
            try (Stream<Path> walk = Files.walk(protocolBufferFileDirectory)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); }
                    catch (IOException e) { throw new UncheckedIOException("Failed to delete: " + p, e); }
                });
            } catch (UncheckedIOException | IOException e) {
                model.addAttribute("error", "Cleanup error: " + e.getMessage());
                return "calculator";
            }
        }
        return null;
    }
}