package com.calculator.application.configuration.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Configuration
public class ProtobufJarConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ProtobufJarConfiguration.class);

    @Bean
    public Path protobufJarPath() throws IOException {
        String javaClassPath = System.getProperty("java.class.path");
        if (javaClassPath != null) {
            for (String entry : javaClassPath.split(File.pathSeparator)) {
                if (entry.contains("protobuf-java")) {
                    log.info("Found protobuf-java on system classpath: {}", entry);
                    return Path.of(entry);
                }
            }
        }

        if (javaClassPath != null) {
            for (String entry : javaClassPath.split(File.pathSeparator)) {
                Path entryPath = Path.of(entry);
                if (Files.isRegularFile(entryPath) && entry.endsWith(".jar")) {
                    Path extracted = extractProtobufFromJar(entryPath);
                    if (extracted != null) {
                        log.info("Extracted protobuf-java from fat jar: {}", extracted);
                        return extracted;
                    }
                }
            }
        }

        throw new IllegalStateException(
                "Could not locate protobuf-java jar for compilation.");
    }

    private Path extractProtobufFromJar(Path fatJarPath) throws IOException {
        try (JarFile jarFile = new JarFile(fatJarPath.toFile())) {
            JarEntry protobufEntry = jarFile.stream()
                    .filter(e -> e.getName().contains("protobuf-java")
                            && e.getName().endsWith(".jar"))
                    .findFirst()
                    .orElse(null);

            if (protobufEntry == null) {
                return null;
            }

            log.info("Found protobuf entry in fat jar: {}", protobufEntry.getName());

            Path tempJar = Files.createTempFile("protobuf-java", ".jar");
            tempJar.toFile().deleteOnExit();

            try (InputStream is = jarFile.getInputStream(protobufEntry)) {
                long bytesCopied = Files.copy(is, tempJar,
                        StandardCopyOption.REPLACE_EXISTING);
                log.info("Extracted {} bytes to {}", bytesCopied, tempJar);

                if (bytesCopied == 0) {
                    throw new IllegalStateException(
                            "Extracted protobuf jar is empty: " + protobufEntry.getName());
                }
            }

            return tempJar;
        }
    }
}