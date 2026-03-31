package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.AWSDataTransferCostCalculationService;
import com.calculator.application.services.calculators.rps.RPSCostCostCalculatorService;
import com.calculator.application.services.calculators.rps.security.jwt.RPSJWTCostCalculator;
import com.calculator.application.services.compilators.CompilationService;
import com.calculator.application.services.protobuf.ProtocolBufferMessageSizeCalculationService;
import com.calculator.application.services.protobuf.ProtocolBufferService;
import com.calculator.domain.dto.compilers.ProtocCompiler;
import com.calculator.domain.dto.properties.ProtoFileFullyQualifiedProperties;
import com.calculator.domain.dto.protofiles.ParsedProtocolBufferFile;
import com.calculator.domain.dto.results.CompilationResult;
import com.calculator.domain.dto.results.JavaCompilationResult;
import com.calculator.domain.dto.results.MessageSizeCalculationResult;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.model.architecture.tactics.gRPC.interceptor.InterceptorType;
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

import static com.calculator.domain.dto.tactics.microservices.SAGAPatternCostMessages.SAGA_PATTERN_COSTS_ALTER_MESSAGE;
import static com.calculator.domain.dto.tactics.resiliency.retry.RetryPatternCostMessages.RETRY_TACTICS_NETWORKING_COST_ALTER_MESSAGE;
import static com.calculator.domain.model.architecture.tactics.security.JWTOverhead.*;
import static com.calculator.infrastructure.web.rest.TacticsSessionController.SESSION_KEY;
import static com.calculator.shared.ProtocolBufferParsedFileUtils.initializeProtoFileFullyQualifiedProperties;
import static com.calculator.shared.ProtocolBufferParsedFileUtils.isValid;

@Controller
public class TCOCalculatorController implements ErrorController {

    private static final double SECONDS_PER_MONTH = 2_592_000.0;
    private static final double BYTES_PER_GB      = 1_073_741_824.0;
    private static final Locale DISPLAY_LOCALE    = Locale.forLanguageTag("en-US");

    private static final String NO_TACTICS_CONFIGURATION_FOUND_MESSAGE =
            """
            Note: No tactic configuration found in your session.
            Results are based on default settings (all tactics disabled).
            Configure tactics and re-upload to get accurate results.
            """;

    @Autowired
    AWSDataTransferCostCalculationService awsDataTransferCostCalculationService;
    @Autowired
    ProtocolBufferService protocolBufferService;
    @Autowired
    CompilationService compilationService;
    @Autowired
    ProtocolBufferMessageSizeCalculationService protocolBufferMessageSizeCalculationService;
    @Autowired
    RPSCostCostCalculatorService requestsPerSecondCalculatorService;
    @Autowired
    RPSJWTCostCalculator rpsjwtCostCalculator;

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
                populateTacticsModel(architecturalDecisionsDTO, effectiveRequestsPerSecond, model);
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

    private void populateTacticsModel(ArchitecturalDecisionsDTO architecturalDecisionsDTO, long effectiveRps, Model model) {

        List<Map<String, String>> infoTactics = new ArrayList<>();
        List<Map<String, String>> rpsTactics  = new ArrayList<>();

        // Reliability
        if (architecturalDecisionsDTO.reliabilityTactics().reliabilityClientSideLoadBalancerTactic()) {
            infoTactics.add(tacticEntry("Client-side Load Balancing", null,
                    "Balances the emission of gRPC packets across server instances.", null));
        }
        if (architecturalDecisionsDTO.reliabilityTactics().reliabilityServerSideLoadBalancerTactic()) {
            infoTactics.add(tacticEntry("Server-side Load Balancing", null,
                    "Balances the reception of gRPC packets across backend replicas.", null));
        }
        // Resiliency
        if (architecturalDecisionsDTO.timeoutTactic().resiliencyTimeoutTactic()) {
            infoTactics.add(tacticEntry("Timeout", architecturalDecisionsDTO.timeoutTactic().tacticTimeoutMilliseconds() + " ms",
                    "Discards lost packets and frees the client thread after the configured wait.", null));
        }
        if (architecturalDecisionsDTO.circuitBreakerTactic().resiliencyCircuitBreakerPattern()) {
            populateCircuitBreakerPattern(architecturalDecisionsDTO, infoTactics);
        }
        // Microservices
        if (architecturalDecisionsDTO.sagaPattern().microservicesSAGAPattern()) {
            populateSAGAPattern(architecturalDecisionsDTO, architecturalDecisionsDTO.requestsPerSecond(), rpsTactics);
        }
        if (architecturalDecisionsDTO.retryTactic().resiliencyRetryTactic()) {
            populateRetryTactic(architecturalDecisionsDTO, architecturalDecisionsDTO.requestsPerSecond(), rpsTactics);
        }
        // Security
        populateSecurityTactics(architecturalDecisionsDTO.securityTactics(), architecturalDecisionsDTO.requestsPerSecond(), infoTactics, rpsTactics);

        model.addAttribute("infoTactics",    infoTactics);
        model.addAttribute("rpsTactics",     rpsTactics);
        model.addAttribute("hasTactics",     !infoTactics.isEmpty() || !rpsTactics.isEmpty());
        model.addAttribute("baseRps",        String.format(DISPLAY_LOCALE, "%,d", architecturalDecisionsDTO.requestsPerSecond()));
        model.addAttribute("effectiveRps",   String.format(DISPLAY_LOCALE, "%,d", effectiveRps));
        model.addAttribute("rpsWasAdjusted", effectiveRps != architecturalDecisionsDTO.requestsPerSecond());
    }

    private void populateCircuitBreakerPattern(ArchitecturalDecisionsDTO architecturalDecisionsDTO,
                                               List<Map<String, String>> infoTactics) {
        String cbConfig = String.format(
                "Min calls: %d | Half-open calls: %d | Wait: %d ms | Failure threshold: %d%%",
                architecturalDecisionsDTO.circuitBreakerTactic().circuitBreakerPatternMinimumCalls(), architecturalDecisionsDTO.circuitBreakerTactic().circuitBreakerHalfOpen(),
                architecturalDecisionsDTO.circuitBreakerTactic().circuitBreakerWaitMilliseconds(), architecturalDecisionsDTO.circuitBreakerTactic().circuitBreakerFailureRate());
        infoTactics.add(tacticEntry("Circuit Breaker", cbConfig,
                "Opens the circuit when failure thresholds are exceeded, protecting downstream services.", null));
    }

    private void populateRetryTactic(ArchitecturalDecisionsDTO architecturalDecisionsDTO, long baseRps,
                                     List<Map<String, String>> rpsTactics) {
        long extra = baseRps * architecturalDecisionsDTO.retryTactic().tacticRetryTimes();
        rpsTactics.add(tacticEntry("Retry", architecturalDecisionsDTO.retryTactic().tacticRetryTimes() + " max retries",
                RETRY_TACTICS_NETWORKING_COST_ALTER_MESSAGE.getMessage(),
                "+" + String.format(DISPLAY_LOCALE, "%,d", extra) + " req/s"));
    }

    private void populateSAGAPattern(ArchitecturalDecisionsDTO architecturalDecisionsDTO, long baseRps,
                                     List<Map<String, String>> rpsTactics) {
        int steps = architecturalDecisionsDTO.sagaPattern().sagaCompensatableTransactions() + architecturalDecisionsDTO.sagaPattern().sagaRetriableTransactions() + architecturalDecisionsDTO.sagaPattern().sagaPivotTransactions();
        String sagaConfig = String.format(
                "Steps per SAGA instance — Compensatable: %d | Retriable: %d | Pivot: %d | Total: %d",
                architecturalDecisionsDTO.sagaPattern().sagaCompensatableTransactions(), architecturalDecisionsDTO.sagaPattern().sagaRetriableTransactions(),
                architecturalDecisionsDTO.sagaPattern().sagaPivotTransactions(), steps);
        String impact = steps > 0
                ? "×" + steps + " steps → " + String.format(DISPLAY_LOCALE, "%,d", baseRps * steps) + " req/s"
                : "No steps configured";
        rpsTactics.add(tacticEntry("SAGA Pattern", sagaConfig, SAGA_PATTERN_COSTS_ALTER_MESSAGE.getMessage(), impact));
    }

    private void populateSecurityTactics(SecurityTactics securityTactics, long baseRequestsPerSecond,
                                         List<Map<String, String>> generalTactics,
                                         List<Map<String, String>> requestsPerSecondModifierTactics) {
        if (securityTactics == null) return;

        // TLS — bytes overhead (per-message) + handshake extra requests
        if (securityTactics.tlsTactic().tlsEnabled()) {
            String tlsByteRange = TLSOverhead.RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX.getFormattedReference();
            long handshakeRps = securityTactics.tlsTactic().extraRequestsPerSecondFromTLSHandshakesAtTheConfiguredReconnectRate();
            if (handshakeRps > 0) {
                requestsPerSecondModifierTactics.add(tacticEntry("TLS — handshake messages",
                        securityTactics.tlsTactic().tlsReconnectsPerHour() + " reconnects/hr × " + TLSOverhead.TLS_HANDSHAKE_MESSAGES.getOverhead() + " messages",
                        "TLS 1.3 handshake (ClientHello + ServerHello/Certificate/Finished) runs once per connection. " +
                                tlsByteRange + ". AWS ACM issues and renews certificates at no additional cost.",
                        "+" + String.format(DISPLAY_LOCALE, "%,d", handshakeRps) + " req/s"));
            } else {
                generalTactics.add(tacticEntry("TLS (one-way)",
                        securityTactics.tlsTactic().tlsReconnectsPerHour() + " reconnects/hr",
                        "TLS 1.3 encrypts every gRPC message. " + tlsByteRange +
                                ". Handshake is connection-scoped — negligible RPS impact at this reconnect rate. AWS ACM certificates are free.", null));
            }
        }

        // mTLS — same byte overhead as TLS, more handshake messages
        if (securityTactics.tlsTactic().mtlsEnabled()) {
            String mtlsByteRange = TLSOverhead.RFC_8446_AES_GCM_AEAD_TLS_WITHOUT_PADDING_OVERHEAD_BYTES_MAX.getFormattedReference();
            long handshakeRps = securityTactics.tlsTactic().extraRequestsPerSecondFromTLSHandshakesAtTheConfiguredReconnectRate();
            String handshakeDetail = String.format(
                    "%d reconnects/hr × %d messages (ClientHello + ServerHello/Cert/CertReq + client Cert + CertVerify + Finished)",
                    securityTactics.tlsTactic().tlsReconnectsPerHour(), TLSOverhead.MTLS_HANDSHAKE_MESSAGES.getOverhead());
            if (handshakeRps > 0) {
                requestsPerSecondModifierTactics.add(tacticEntry("mTLS — handshake messages",
                        handshakeDetail,
                        "mTLS adds client certificate exchange and CA validation to the TLS handshake. " +
                                mtlsByteRange + ". Per-message overhead is identical to TLS. AWS ACM certificates are free.",
                        "+" + String.format(DISPLAY_LOCALE, "%,d", handshakeRps) + " req/s"));
            } else {
                generalTactics.add(tacticEntry("mTLS (mutual TLS)",
                        securityTactics.tlsTactic().tlsReconnectsPerHour() + " reconnects/hr",
                        "Both client and server present X.509 certificates. CA validates both. " +
                                mtlsByteRange + ". Handshake is connection-scoped — negligible RPS impact at this reconnect rate. AWS ACM certificates are free.",
                        null));
            }
        }

        // Basic Auth — bytes overhead only, no extra requests
        if (securityTactics.basicAuthenticationPattern().basicAuthEnabled()) {
            generalTactics.add(tacticEntry("Basic Authentication ⚠",
                    "~50–100 bytes per request header",
                    "Authorization: Basic base64(username:password) is sent with every request. " +
                            "No extra requests, but the credential is valid until the password changes — " +
                            "there is no token expiry or revocation mechanism. Not recommended for production gRPC APIs. " +
                            "Prefer OAuth 2.0 + JWT for time-bounded, revocable access control.", null));
        }

        // OAuth 2.0 + JWT — bytes overhead on requests + extra requests for acquisition and introspection
        if (securityTactics.jwtTactic().oauthJwtEnabled()) {
            int ttl     = securityTactics.jwtTactic().tokenTtlSeconds()   > 0 ? securityTactics.jwtTactic().tokenTtlSeconds() : 3600;
            int clients = securityTactics.jwtTactic().concurrentClients()  > 0 ? securityTactics.jwtTactic().concurrentClients() : 1;

            String interceptorLabel = securityTactics.jwtTactic().interceptorType() == InterceptorType.UNARY
                    ? "Unary interceptor" : "Stream interceptor";
            String jwtByteRange = String.format(
                    "JWT Bearer header \n RFC 7515 Section-7.1 \n BASE64URL(UTF8(JWS Protected Header)) \n || '.' || * BASE64URL(JWS Payload) \n || '.' || * BASE64URL(JWS Signature)  \n adds %d bytes typical \n due to %s \n",
                    JWT_OVERHEAD_BYTES_TYPICAL.getOverhead(), JWT_OVERHEAD_BYTES_TYPICAL.getFormattedReference()
            );

            long tokenAcqRps = rpsjwtCostCalculator.extraRequestsPerSecondFromOAuthTokenAcquisitionCallsToTheAuthorisationServer(baseRequestsPerSecond, securityTactics.jwtTactic());
            long introspectionRps = rpsjwtCostCalculator.extraRequestsPerSecondFromRemoteTokenIntrospection(baseRequestsPerSecond, securityTactics.jwtTactic());

            // JWT header byte overhead is always informational (bytes, not extra requests)
            generalTactics.add(tacticEntry("OAuth 2.0 + JWT — bearer token",
                    interceptorLabel + " | TTL " + ttl + "s | " + clients + " client(s)",
                    jwtByteRange + ". The token travels in every request's Authorization metadata header. " +
                            "Response messages carry no JWT overhead. Validation enforced by the configured gRPC interceptor.", null));

            // Token acquisition — extra requests to the authorisation server
            if (tokenAcqRps > 0) {
                requestsPerSecondModifierTactics.add(tacticEntry("OAuth 2.0 — token acquisition",
                        "1 call per TTL ÷ " + clients + " client(s) — RFC 6749 §4.1",
                        "Periodic call to the authorisation server to obtain a new JWT. " +
                                "Amortised across the token TTL and the number of concurrent clients sharing the token.",
                        "+" + String.format(DISPLAY_LOCALE, "%,d", tokenAcqRps) + " req/s"));
            }

            // Remote introspection — one extra call per incoming gRPC request
            if (introspectionRps > 0) {
                requestsPerSecondModifierTactics.add(tacticEntry("OAuth 2.0 — remote introspection",
                        "1 introspection call per gRPC request — RFC 7662",
                        "Each incoming gRPC request triggers a synchronous token introspection call to the " +
                                "authorisation server. This doubles outbound request volume. " +
                                "Switching to local JWT signature validation eliminates this overhead entirely.",
                        "+" + String.format(DISPLAY_LOCALE, "%,d", introspectionRps) + " req/s"));
            }
        }
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
        double dataTransferCostUsd = awsDataTransferCostCalculationService.calculateDataTransferCost(responseGbPerMonth);

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, String> tacticEntry(String name, String value, String description,
                                            String rpsImpact) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("name",        name);
        entry.put("value",       value != null ? value : "—");
        entry.put("description", description);
        entry.put("rpsImpact",   rpsImpact != null ? rpsImpact : "");
        return entry;
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