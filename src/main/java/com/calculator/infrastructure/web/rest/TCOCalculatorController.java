package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.AWSDataTransferCostCalculationService;
import com.calculator.application.services.compilator.CompilationService;
import com.calculator.application.services.ProtocolBufferMessageSizeCalculationService;
import com.calculator.application.services.ProtocolBufferService;
import com.calculator.domain.model.compilers.ProtocCompiler;
import com.calculator.domain.model.properties.ProtoFileFullyQualifiedProperties;
import com.calculator.domain.model.protofiles.ParsedProtocolBufferFile;
import com.calculator.domain.model.results.CompilationResult;
import com.calculator.domain.model.results.JavaCompilationResult;
import com.calculator.domain.model.results.MessageSizeCalculationResult;
import com.calculator.domain.model.tactics.ArchitecturalTacticsContext;
import com.calculator.domain.model.tactics.TacticsConfigDTO;
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
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static com.calculator.shared.ProtocolBufferParsedFileUtils.initializeProtoFileFullyQualifiedProperties;
import static com.calculator.shared.ProtocolBufferParsedFileUtils.isValid;

@Controller
public class TCOCalculatorController implements ErrorController {

    private static final double SECONDS_PER_MONTH = 2_592_000.0;
    private static final double BYTES_PER_GB      = 1_073_741_824.0;
    private static final Locale DISPLAY_LOCALE    = Locale.forLanguageTag("es-ES");
    private static final String NO_TACTICS_CONFIGURATION_FOUND_MESSAGE =
            """ 
            Note: No tactic configuration found in your session.
            Results are based on default settings (all tactics disabled).
            Configure tactics and re-upload to get accurate results.
            """;
    public static final String RETRY_TACTICS_NETWORKING_COST_ALTER_MESSAGE =
            """
            Retries gRPC packet emission under temporary outages. Increases networking costs
            as up to N additional packets may be sent per logical request.
            """;
    public static final String SAGA_PATTERN_COSTS_ALTER_MESSAGE =
            """
            Each logical request triggers one SAGA instance. Every step in that instance 
            is an independent gRPC call, so actual traffic = base RPS × steps per instance.
            """;

    @Autowired
    AWSDataTransferCostCalculationService awsDataTransferCostCalculationService;

    @Autowired
    ProtocolBufferService protocolBufferService;

    @Autowired
    CompilationService compilationService;

    @Autowired
    ProtocolBufferMessageSizeCalculationService protocolBufferMessageSizeCalculationService;

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

        TacticsConfigDTO tacticsConfiguration = (TacticsConfigDTO) session.getAttribute(SESSION_KEY);
        if (tacticsConfiguration == null) {
            tacticsConfiguration = TacticsConfigDTO.empty();
            model.addAttribute("uploadMessage",
                    NO_TACTICS_CONFIGURATION_FOUND_MESSAGE);
        }

        ArchitecturalTacticsContext tactics = tacticsConfiguration.toTacticsContext();
        String view = calculateTCO(tacticsConfiguration.requestsPerSecond(), protoFile, tactics, model);
        return (view != null) ? view : "calculator";
    }

    private String calculateTCO(long requestsPerSecond, MultipartFile protoFile,
                                ArchitecturalTacticsContext tactics, Model model) {
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

                JavaCompilationResult javaResult =
                        compile(model, protoCompiler, protoProps.fullRequestMessageClassName());
                if (javaResult.errorCompilationMessage() != null)
                    return javaResult.errorCompilationMessage();

                URLClassLoader classLoader = javaResult.compilationResult().classLoader();

                long effectiveRequestsPerSecond = compute(requestsPerSecond, tactics);
                populateTacticsModel(tactics, requestsPerSecond, effectiveRequestsPerSecond, model);
                showTCONetworkingCosts(effectiveRequestsPerSecond, classLoader, model, protoProps);

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

    private long compute(long baseRequestsPerSecond, ArchitecturalTacticsContext architecturalTactics) {
        long effectiveRequestsPerSecond = baseRequestsPerSecond;

        if (architecturalTactics.microservicesSAGAPattern()) {
            effectiveRequestsPerSecond = computeSAGAPattern(baseRequestsPerSecond, architecturalTactics, effectiveRequestsPerSecond);
        }

        if (architecturalTactics.resiliencyRetryTactic() && architecturalTactics.tacticRetryTimes() > 0) {
            effectiveRequestsPerSecond += baseRequestsPerSecond * architecturalTactics.tacticRetryTimes();
        }

        return effectiveRequestsPerSecond;
    }

    private static long computeSAGAPattern(long baseRequestsPerSecond, ArchitecturalTacticsContext architecturalTactics, long effective) {
        int steps = architecturalTactics.sagaCompensatableTransactions() + architecturalTactics.sagaRetriableTransactions() + architecturalTactics.sagaPivotTransactions();
        if (steps > 0) {
            effective = baseRequestsPerSecond * steps;
        }
        return effective;
    }

    private void populateTacticsModel(ArchitecturalTacticsContext architecturalTactics, long baseRequestsPerSecond, long effectiveRequestsPerSecond, Model model) {

        List<Map<String, String>> infoTactics = new ArrayList<>();
        List<Map<String, String>> requestsPerSecondsTactics  = new ArrayList<>();

        if (architecturalTactics.reliabilityClientSideLoadBalancerTactic()) {
            infoTactics.add(tacticEntry("Client-side Load Balancing", null,
                    "Balances the emission of gRPC packets across server instances."));
        }
        if (architecturalTactics.reliabilityServerSideLoadBalancerTactic()) {
            infoTactics.add(tacticEntry("Server-side Load Balancing", null,
                    "Balances the reception of gRPC packets across backend replicas."));
        }
        if (architecturalTactics.resiliencyTimeoutTactic()) {
            infoTactics.add(tacticEntry("Timeout", architecturalTactics.tacticTimeoutMilliseconds() + " ms",
                    "Discards lost packets and frees the client thread after the configured wait."));
        }
        if (architecturalTactics.resiliencyCircuitBreakerPattern()) {
            populateCircuitBreakerPattern(architecturalTactics, infoTactics);
        }

        if (architecturalTactics.microservicesSAGAPattern()) {
            populateSAGAPattern(architecturalTactics, baseRequestsPerSecond, requestsPerSecondsTactics);
        }

        if (architecturalTactics.resiliencyRetryTactic()) {
            populateRetryTactic(architecturalTactics, baseRequestsPerSecond, requestsPerSecondsTactics);
        }

        model.addAttribute("infoTactics",    infoTactics);
        model.addAttribute("rpsTactics",     requestsPerSecondsTactics);
        model.addAttribute("hasTactics",     !infoTactics.isEmpty() || !requestsPerSecondsTactics.isEmpty());
        model.addAttribute("baseRps",        String.format(DISPLAY_LOCALE, "%,d", baseRequestsPerSecond));
        model.addAttribute("effectiveRps",   String.format(DISPLAY_LOCALE, "%,d", effectiveRequestsPerSecond));
        model.addAttribute("rpsWasAdjusted", effectiveRequestsPerSecond != baseRequestsPerSecond);
    }

    private void populateCircuitBreakerPattern(ArchitecturalTacticsContext architecturalTactics, List<Map<String, String>> infoTactics) {
        String cbConfig = String.format(
                "Min calls: %d | Half-open calls: %d | Wait: %d ms | Failure threshold: %d%%",
                architecturalTactics.circuitBreakerPatternMinimumCalls(), architecturalTactics.circuitBreakerHalfOpen(), architecturalTactics.circuitBreakerWaitMilliseconds(), architecturalTactics.circuitBreakerFailureRate());
        infoTactics.add(tacticEntry("Circuit Breaker", cbConfig,
                "Opens the circuit when failure thresholds are exceeded, protecting downstream services."));
    }

    private void populateRetryTactic(ArchitecturalTacticsContext architecturalTactics, long baseRequestsPerSecond, List<Map<String, String>> requestsPerSecondTactics) {
        long retryExtra = baseRequestsPerSecond * architecturalTactics.tacticRetryTimes();
        requestsPerSecondTactics.add(tacticEntry("Retry", architecturalTactics.tacticRetryTimes() + " max retries",
                RETRY_TACTICS_NETWORKING_COST_ALTER_MESSAGE,
                "+" + String.format(DISPLAY_LOCALE, "%,d", retryExtra) + " req/s"));
    }

    private void populateSAGAPattern(ArchitecturalTacticsContext architecturalTactics, long baseRequestsPerSecond, List<Map<String, String>> requestsPerSecondTactics) {
        int steps = architecturalTactics.sagaCompensatableTransactions() + architecturalTactics.sagaRetriableTransactions() + architecturalTactics.sagaPivotTransactions();
        String sagaConfig = String.format(
                "Steps per SAGA instance — Compensatable: %d | Retriable: %d | Pivot: %d | Total: %d",
                architecturalTactics.sagaCompensatableTransactions(), architecturalTactics.sagaRetriableTransactions(), architecturalTactics.sagaPivotTransactions(), steps);
        String impact = steps > 0
                ? "×" + steps + " steps → " + String.format(DISPLAY_LOCALE, "%,d", baseRequestsPerSecond * steps) + " req/s"
                : "No steps configured";
        requestsPerSecondTactics.add(tacticEntry("SAGA Pattern", sagaConfig,
                SAGA_PATTERN_COSTS_ALTER_MESSAGE,
                impact));
    }

    private Map<String, String> tacticEntry(String name, String value, String description) {
        return tacticEntry(name, value, description, null);
    }

    private Map<String, String> tacticEntry(String name, String value, String description, String requestsPerSecondImpact) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("name",        name);
        entry.put("value",       value != null ? value : "—");
        entry.put("description", description);
        entry.put("rpsImpact",   requestsPerSecondImpact != null ? requestsPerSecondImpact : "");
        return entry;
    }

    private void showTCONetworkingCosts(long effectiveRequestsPerSecond, URLClassLoader classLoader,
                                        Model model, ProtoFileFullyQualifiedProperties protoProps) {
        MessageSizeCalculationResult requestMessageSize  = calculateRequestMessageSize(model,  classLoader, protoProps.fullRequestMessageClassName());
        MessageSizeCalculationResult responseMessageSize = calculateResponseMessageSize(model, classLoader, protoProps.fullResponseMessageClassName());

        long requestsPerMonth = (long) (effectiveRequestsPerSecond * SECONDS_PER_MONTH);
        double requestGbPerMonth   = (requestMessageSize.size()  * effectiveRequestsPerSecond * SECONDS_PER_MONTH) / BYTES_PER_GB;
        double responseGbPerMonth  = (responseMessageSize.size() * effectiveRequestsPerSecond * SECONDS_PER_MONTH) / BYTES_PER_GB;
        double dataTransferCostUsd = awsDataTransferCostCalculationService.calculateDataTransferCost(responseGbPerMonth);

        model.addAttribute("requestsPerMonth",    String.format(DISPLAY_LOCALE, "%,d",   requestsPerMonth));
        model.addAttribute("requestGbPerMonth",   String.format(DISPLAY_LOCALE, "%.4f",  requestGbPerMonth));
        model.addAttribute("responseGbPerMonth",  String.format(DISPLAY_LOCALE, "%.4f",  responseGbPerMonth));
        model.addAttribute("dataTransferCostUsd", String.format(DISPLAY_LOCALE, "%,.2f", dataTransferCostUsd));
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
        MessageSizeCalculationResult messageSizeCalculationResult = new MessageSizeCalculationResult(null, 0);
        try {
            messageSizeCalculationResult = protocolBufferMessageSizeCalculationService.getMessageSize(classLoader, fullClassName);
            model.addAttribute("requestSize", messageSizeCalculationResult.size());
        } catch (Exception e) {
            log.warning(e.getMessage());
            model.addAttribute("requestSize", 0);
            model.addAttribute("requestMessageError", "Failed to calculate max request size: " + e.getMessage());
        }
        return messageSizeCalculationResult;
    }

    private MessageSizeCalculationResult calculateResponseMessageSize(
            Model model, URLClassLoader classLoader, String fullClassName) {
        MessageSizeCalculationResult messageSizeCalculationResult = new MessageSizeCalculationResult(null, 0);
        try {
            messageSizeCalculationResult = protocolBufferMessageSizeCalculationService.getMessageSize(classLoader, fullClassName);
            model.addAttribute("responseSize", messageSizeCalculationResult.size());
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
        return messageSizeCalculationResult;
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
                    try {
                        Files.deleteIfExists(p);
                    }
                    catch (IOException e) {
                        throw new UncheckedIOException("Failed to delete: " + p, e);
                    }
                });
            } catch (UncheckedIOException | IOException e) {
                model.addAttribute("error", "Cleanup error: " + e.getMessage());
                return "calculator";
            }
        }
        return null;
    }
}