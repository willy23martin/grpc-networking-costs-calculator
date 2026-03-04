package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.AWSDataTransferCostCalculationService;
import com.calculator.application.services.compilator.CompilationService;
import com.calculator.application.services.ProtocolBufferMessageSizeCalculationService;
import com.calculator.application.services.ProtocolBufferService;
import com.calculator.domain.model.compilers.ProtocCompiler;
import com.calculator.domain.model.properties.ProtoFileFullyQualifiedProperties;
import com.calculator.domain.model.protofiles.ParsedProtocolBufferFile;
import com.calculator.domain.model.request.TCOCalculatorRequestDTO;
import com.calculator.domain.model.results.CompilationResult;
import com.calculator.domain.model.results.JavaCompilationResult;
import com.calculator.domain.model.results.MessageSizeCalculationResult;
import com.calculator.domain.model.tactics.ArchitecturalTacticsContext;
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

import static com.calculator.shared.ProtocolBufferParsedFileUtils.initializeProtoFileFullyQualifiedProperties;
import static com.calculator.shared.ProtocolBufferParsedFileUtils.isValid;

@Controller
public class TCOCalculatorController implements ErrorController {

    private static final double SECONDS_PER_MONTH = 2_592_000.0;
    private static final double BYTES_PER_GB      = 1_073_741_824.0;
    private static final Locale DISPLAY_LOCALE    = Locale.forLanguageTag("es-ES");

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
    public String calculateProtoFileTCONetworkingCosts(TCOCalculatorRequestDTO request, Model model) {

        if (request.getProtoFile() == null || request.getProtoFile().isEmpty()) {
            model.addAttribute("uploadMessage", "No file selected for upload.");
            return "calculator";
        }

        // Use the record's mapping method
        ArchitecturalTacticsContext tactics = request.toTacticsContext();

        String view = calculateTCO(request.getRequestsPerSecond(), request.getProtoFile(), tactics, model);

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

                // Compute effective RPS after tactics adjustments, then use it for TCO
                long effectiveRps = computeEffectiveRps(requestsPerSecond, tactics);
                populateTacticsModel(tactics, requestsPerSecond, effectiveRps, model);
                showTCONetworkingCosts(effectiveRps, classLoader, model, protoProps);

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

    // ── Effective RPS after tactics ───────────────────────────────
    //
    // Two tactics can increase the number of actual gRPC calls per second:
    //
    // RETRY: Each logical request may generate up to N extra retries on failure.
    //   effectiveRps += baseRps * retryTimes
    //
    // SAGA: Each logical request triggers exactly one SAGA instance.
    //   Every step in that instance (compensatable, retriable, pivot) is an
    //   independent gRPC call to a different microservice.
    //   effectiveRps = baseRps * stepsPerSagaInstance  (replaces, not adds to, base)
    //
    //   Example: 1,000 RPS × 3 steps = 3,000 gRPC calls/sec.
    //   This is NOT 1,000 + 3,000 = 4,000. The 3,000 already includes the original
    //   1,000 — each of the 1,000 requests simply fans out into 3 calls.
    //
    // When both Retry and SAGA are active, SAGA multiplication is applied first,
    // then retry additions are applied on top of the SAGA-multiplied value.
    private long computeEffectiveRps(long baseRps, ArchitecturalTacticsContext t) {
        long effective = baseRps;

        // SAGA: replace base with multiplied value
        if (t.tacticSaga()) {
            int steps = t.tacticSagaCompensatable() + t.tacticSagaRetriable() + t.tacticSagaPivot();
            if (steps > 0) {
                effective = baseRps * steps;
            }
        }

        // RETRY: additive on top of effective (which may already be SAGA-multiplied)
        if (t.tacticRetry() && t.tacticRetryTimes() > 0) {
            effective += baseRps * t.tacticRetryTimes();
        }

        return effective;
    }

    private void populateTacticsModel(ArchitecturalTacticsContext t, long baseRps, long effectiveRps, Model model) {

        List<Map<String, String>> infoTactics = new ArrayList<>();
        List<Map<String, String>> rpsTactics  = new ArrayList<>();

        // ── No-RPS-impact tactics ────────────────────────────────
        if (t.tacticClientLb()) {
            infoTactics.add(tacticEntry("Client-side Load Balancing", null,
                    "Balances the emission of gRPC packets across server instances."));
        }
        if (t.tacticServerLb()) {
            infoTactics.add(tacticEntry("Server-side Load Balancing", null,
                    "Balances the reception of gRPC packets across backend replicas."));
        }
        if (t.tacticTimeout()) {
            infoTactics.add(tacticEntry("Timeout", t.tacticTimeoutMs() + " ms",
                    "Discards lost packets and frees the client thread after the configured wait."));
        }
        if (t.tacticCb()) {
            String cbConfig = String.format(
                    "Min calls: %d | Half-open calls: %d | Wait: %d ms | Failure threshold: %d%%",
                    t.tacticCbMinCalls(), t.tacticCbHalfOpen(), t.tacticCbWaitMs(), t.tacticCbFailureRate());
            infoTactics.add(tacticEntry("Circuit Breaker", cbConfig,
                    "Opens the circuit when failure thresholds are exceeded, protecting downstream services."));
        }

        // ── RPS-impacting tactics ────────────────────────────────

        // SAGA — multiplicative: each request fans out into N gRPC calls
        if (t.tacticSaga()) {
            int steps = t.tacticSagaCompensatable() + t.tacticSagaRetriable() + t.tacticSagaPivot();
            String sagaConfig = String.format(
                    "Steps per SAGA instance — Compensatable: %d | Retriable: %d | Pivot: %d | Total: %d",
                    t.tacticSagaCompensatable(), t.tacticSagaRetriable(), t.tacticSagaPivot(), steps);
            String impact = steps > 0
                    ? "×" + steps + " steps → " + String.format(DISPLAY_LOCALE, "%,d", baseRps * steps) + " req/s"
                    : "No steps configured";
            rpsTactics.add(tacticEntry("SAGA Pattern", sagaConfig,
                    "Each logical request triggers one SAGA instance. Every step in that instance " +
                            "is an independent gRPC call, so actual traffic = base RPS × steps per instance.",
                    impact));
        }

        // RETRY — additive: extra packets on top of existing traffic
        if (t.tacticRetry()) {
            long retryExtra = baseRps * t.tacticRetryTimes();
            rpsTactics.add(tacticEntry("Retry", t.tacticRetryTimes() + " max retries",
                    "Retries gRPC packet emission under temporary outages. Increases networking costs " +
                            "as up to N additional packets may be sent per logical request.",
                    "+" + String.format(DISPLAY_LOCALE, "%,d", retryExtra) + " req/s"));
        }

        model.addAttribute("infoTactics",    infoTactics);
        model.addAttribute("rpsTactics",     rpsTactics);
        model.addAttribute("hasTactics",     !infoTactics.isEmpty() || !rpsTactics.isEmpty());
        model.addAttribute("baseRps",        String.format(DISPLAY_LOCALE, "%,d", baseRps));
        model.addAttribute("effectiveRps",   String.format(DISPLAY_LOCALE, "%,d", effectiveRps));
        model.addAttribute("rpsWasAdjusted", effectiveRps != baseRps);
    }

    private Map<String, String> tacticEntry(String name, String value, String description) {
        return tacticEntry(name, value, description, null);
    }

    private Map<String, String> tacticEntry(String name, String value, String description, String rpsImpact) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("name",        name);
        entry.put("value",       value != null ? value : "—");
        entry.put("description", description);
        entry.put("rpsImpact",   rpsImpact != null ? rpsImpact : "");
        return entry;
    }

    // ── TCO costs ─────────────────────────────────────────────────
    private void showTCONetworkingCosts(long effectiveRps, URLClassLoader classLoader,
                                        Model model, ProtoFileFullyQualifiedProperties protoProps) {
        MessageSizeCalculationResult req  = calculateRequestMessageSize(model,  classLoader, protoProps.fullRequestMessageClassName());
        MessageSizeCalculationResult resp = calculateResponseMessageSize(model, classLoader, protoProps.fullResponseMessageClassName());

        long   requestsPerMonth    = (long) (effectiveRps * SECONDS_PER_MONTH);
        double requestGbPerMonth   = (req.size()  * effectiveRps * SECONDS_PER_MONTH) / BYTES_PER_GB;
        double responseGbPerMonth  = (resp.size() * effectiveRps * SECONDS_PER_MONTH) / BYTES_PER_GB;
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