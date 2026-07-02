package com.calculator.shared;

import com.calculator.domain.dto.protofiles.JavaParsedProtoFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import static com.calculator.shared.ProtocolBufferFilesSyntaxValidators.*;

public class ProtocolBufferFileParser {

    private static final java.util.logging.Logger log = Logger.getLogger(ProtocolBufferFileParser.class.getName());

    public static JavaParsedProtoFile parseProtoFileFrom(Path protoPath) throws IOException {
        String javaPackageName = "";
        String outerClassName = "";
        String requestMessageSimpleName = "";
        String responseMessageSimpleName = "";

        try (BufferedReader reader = Files.newBufferedReader(protoPath)) {
            String line;
            while ((line = reader.readLine()) != null) {

                Matcher javaPackageMatcher = javaPackageOptionPattern.matcher(line);
                if (javaPackageMatcher.find()) {
                    javaPackageName = javaPackageMatcher.group(1);
                    log.info("Found java_package option: " + javaPackageName);
                }

                Matcher outerClassnameMatcher = javaOuterClassnamePattern.matcher(line);
                if (outerClassnameMatcher.find()) {
                    outerClassName = outerClassnameMatcher.group(1);
                    log.info("Found java_outer_classname option: " + outerClassName);
                }

                Matcher rpcMatcher = rpcMethodPattern.matcher(line);
                if (rpcMatcher.find() && requestMessageSimpleName.isEmpty()) {
                    requestMessageSimpleName = rpcMatcher.group(1);
                    responseMessageSimpleName = rpcMatcher.group(2);
                   log.info("Found first RPC Request message: " + requestMessageSimpleName);
                   log.info("Found first RPC Response message: " + responseMessageSimpleName);
                }
            }
        }

        return new JavaParsedProtoFile(javaPackageName, outerClassName, requestMessageSimpleName, responseMessageSimpleName);
    }
}
