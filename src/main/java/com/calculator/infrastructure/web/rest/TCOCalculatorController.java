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
import java.util.Comparator;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.calculator.shared.ProtocolBufferParsedFileUtils.initializeProtoFileFullyQualifiedProperties;
import static com.calculator.shared.ProtocolBufferParsedFileUtils.isValid;

@Controller
public class TCOCalculatorController implements ErrorController {

    private static final double SECONDS_PER_MONTH = 2_592_000.0; // 30 * 24 * 60 * 60
    private static final double BYTES_PER_GB = 1_073_741_824.0;  // 2^30
    private static final Locale DISPLAY_LOCALE = Locale.forLanguageTag("es-ES");

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
        return "calculator"; // Because with Thymeleaf and SpringMVC we just need to indicate where our view is located, in directory: src/main/resources/templates/calculator.html
    }

    @PostMapping("/calculateTCO")
    public String calculateProtoFileTCONetworkingCosts(@RequestParam("protoFile") MultipartFile protoFile, @RequestParam("requestsPerSecond") long requestsPerSecond, Model model) {

        if (!protoFile.isEmpty()) {
            String bytesSizeResponse = calculateProtoFileTCONetworkingCosts(requestsPerSecond, protoFile, model);
            if (bytesSizeResponse != null) return bytesSizeResponse;
        } else {
            model.addAttribute("uploadMessage", "No file selected for upload.");
            model.addAttribute("requestMessageBytes", "No file selected for upload.");
            model.addAttribute("responseMessageBytes", "No file selected for upload.");
        }

        return "calculator";
    }

    private String calculateProtoFileTCONetworkingCosts(long requestsPerSecond, MultipartFile protoFile, Model model) {
        try {
            updateBytesSizeWithProtoFileSize(protoFile, model);

            Path protocolBufferFileDirectory = null;
            try {
                ParsedProtocolBufferFile parsedProtocolBufferFile = protocolBufferService.parse(protoFile);
                protocolBufferFileDirectory = parsedProtocolBufferFile.protocolBufferFileDirectory();
                if (!isValid(parsedProtocolBufferFile.javaParsedProtoFile())) {
                    model.addAttribute("error", "Could not find a valid RPC definition to extract Request and Response message types from the .proto file.");
                    return "calculator";
                }

                ProtoFileFullyQualifiedProperties protoFileFullyQualifiedProperties = initializeProtoFileFullyQualifiedProperties(parsedProtocolBufferFile.javaParsedProtoFile());
                System.out.println("Attempting to load request class: " + protoFileFullyQualifiedProperties.fullRequestMessageClassName());

                ProtocCompiler protoCompiler = executeProtoc(model, parsedProtocolBufferFile);
                if (protoCompiler == null) return "calculator";

                JavaCompilationResult javaCompilationResult = compile(model, protoCompiler, protoFileFullyQualifiedProperties.fullRequestMessageClassName());
                if (javaCompilationResult.errorCompilationMessage() != null) {
                    return javaCompilationResult.errorCompilationMessage();
                }
                URLClassLoader classLoader = javaCompilationResult.compilationResult().classLoader();

                showTCONetworkingCosts(requestsPerSecond, classLoader, model, protoFileFullyQualifiedProperties);

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

    private ProtocCompiler executeProtoc(Model model, ParsedProtocolBufferFile parsedProtocolBufferFile) throws IOException, InterruptedException {
        ProtocCompiler protoCompiler = protocolBufferService.runProtocOver(parsedProtocolBufferFile);
        int exit = protoCompiler.protoc().waitFor();
        if (exit != 0) {
            String error = new String(protoCompiler.protoc().getInputStream().readAllBytes());
            model.addAttribute("error", "protoc failed: " + error);
            return null;
        }
        return protoCompiler;
    }

    private JavaCompilationResult compile(Model model, ProtocCompiler protoCompiler, String fullClassNameForRequestMessage) throws IOException, ClassNotFoundException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            model.addAttribute("error", "No Java compiler available in this environment.");
            return new JavaCompilationResult("calculator", null);
        }
        CompilationResult compilationResult = compilationService.compile(protoCompiler, compiler, fullClassNameForRequestMessage);
        if (compilationResult.compilationResult() != 0) {
            model.addAttribute("error", "Compilation of generated Java code failed.");
            return new JavaCompilationResult("calculator", compilationResult);
        }
        return new JavaCompilationResult(null, compilationResult);
    }

    private void showTCONetworkingCosts(long requestsPerSecond, URLClassLoader classLoader, Model model, ProtoFileFullyQualifiedProperties protoFileFullyQualifiedProperties) {

        MessageSizeCalculationResult requestMessageSizeCalculationResult = calculateRequestMessageSize(model, classLoader, protoFileFullyQualifiedProperties.fullRequestMessageClassName());
        MessageSizeCalculationResult responseMessageSizeCalculationResult = calculateResponseMessageSize(model, classLoader, protoFileFullyQualifiedProperties.fullResponseMessageClassName());

        long requestsPerMonth = (long) (requestsPerSecond * SECONDS_PER_MONTH);
        double requestGbPerMonth  = (requestMessageSizeCalculationResult.size()  * requestsPerSecond * SECONDS_PER_MONTH) / BYTES_PER_GB;
        double responseGbPerMonth = (responseMessageSizeCalculationResult.size() * requestsPerSecond * SECONDS_PER_MONTH) / BYTES_PER_GB;

        double dataTransferCostUsd = awsDataTransferCostCalculationService.calculateDataTransferCost(responseGbPerMonth);

        model.addAttribute("requestsPerMonth", String.format(DISPLAY_LOCALE, "%,d", requestsPerMonth));
        model.addAttribute("requestGbPerMonth", String.format(DISPLAY_LOCALE, "%.4f", requestGbPerMonth));
        model.addAttribute("responseGbPerMonth", String.format(DISPLAY_LOCALE, "%.4f", responseGbPerMonth));
        model.addAttribute("dataTransferCostUsd", String.format(DISPLAY_LOCALE, "%,.2f", dataTransferCostUsd));
    }

    private MessageSizeCalculationResult calculateResponseMessageSize(Model model, URLClassLoader classLoader, String fullResponseMessageClassName) {
        MessageSizeCalculationResult responseMessageSizeCalculationResult = new MessageSizeCalculationResult(null, 0);
        try {
            responseMessageSizeCalculationResult = protocolBufferMessageSizeCalculationService.getMessageSize(classLoader, fullResponseMessageClassName);
            model.addAttribute("responseSize", responseMessageSizeCalculationResult.size());
            System.out.println("Response message (" + responseMessageSizeCalculationResult.messageClass().getSimpleName() + ") MAX size calculated: " + responseMessageSizeCalculationResult.size() + " bytes.");
        } catch (ClassNotFoundException e) {
            String errorMsg = "Could not load response message: '" + fullResponseMessageClassName + "'. Make sure it exists in your .proto file and all dependencies are compiled.";
            System.err.println(errorMsg);
            log.warning(e.getMessage());
            model.addAttribute("responseSize", 0); // Because response size couldn't be calculated
            model.addAttribute("responseMessageError", errorMsg + " Error details: " + e.getMessage());
        }
        catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            System.err.println("Error accessing methods for response message: " + e.getMessage());
            log.warning(e.getMessage());
            model.addAttribute("responseSize", 0); // Because response size couldn't be calculated
            model.addAttribute("responseMessageError", "Error processing response message: " + e.getMessage());
        }
        catch (Exception e) {
            System.err.println("An unexpected error occurred during response message calculation: " + e.getMessage());
            log.warning(e.getMessage());
            model.addAttribute("responseSize", 0); // Because response size couldn't be calculated
            model.addAttribute("responseMessageError", "An unexpected error occurred during response calculation: " + e.getMessage());
        }
        return responseMessageSizeCalculationResult;
    }

    private MessageSizeCalculationResult calculateRequestMessageSize(Model model, URLClassLoader classLoader, String fullClassNameForRequestMessage) {
        MessageSizeCalculationResult requestMessageSizeCalculationResult = new MessageSizeCalculationResult(null, 0);
        try {

            requestMessageSizeCalculationResult = protocolBufferMessageSizeCalculationService.getMessageSize(classLoader, fullClassNameForRequestMessage);

            model.addAttribute("requestSize", requestMessageSizeCalculationResult.size());
            System.out.println("Request message (" + requestMessageSizeCalculationResult.messageClass().getSimpleName() + ") MAX size calculated: " + requestMessageSizeCalculationResult.size() + " bytes.");

        } catch (Exception e) {
            System.err.println("Error calculating request message MAX size: " + e.getMessage());
            log.warning(e.getMessage());
            model.addAttribute("requestSize", 0); // Because request size couldn't be calculated
            model.addAttribute("requestMessageError", "Failed to calculate max request size: " + e.getMessage());
        }
        return requestMessageSizeCalculationResult;
    }

    private static void updateBytesSizeWithProtoFileSize(MultipartFile protoFile, Model model) {
        String fileName = protoFile.getOriginalFilename();
        long protoFileSize = protoFile.getSize();
        model.addAttribute("uploadMessage", "File '" + fileName + "' uploaded successfully!");
        model.addAttribute("requestMessageBytes", "Request Message bytes: " + protoFileSize + " calculated successfully.");
        model.addAttribute("responseMessageBytes", "Response Message bytes: " + protoFileSize + " calculated successfully.");
    }

    private static String cleanUp(Model model, Path protocolBufferFileDirectory) {
        if (protocolBufferFileDirectory != null) {
            try (Stream<Path> walk = Files.walk(protocolBufferFileDirectory)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException e) {
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
