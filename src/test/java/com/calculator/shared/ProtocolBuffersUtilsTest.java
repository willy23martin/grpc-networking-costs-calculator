package com.calculator.shared;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolBuffersUtilsTest {

    @Test
    void importGoogleProtocolBufferFileDependencies_allFilesCreated(@TempDir Path tempDir) throws IOException {
        // Act: Public method triggers resolve() for all 4 files
        Path result = ProtocolBuffersUtils.importGoogleProtocolBufferFileDependencies(tempDir);

        // Assert: Directory created + all 4 files present (covers resolve() try-catch)
        Path googleDir = tempDir.resolve("google/protobuf");
        assertAll("All well-known types copied",
                () -> assertTrue(Files.exists(googleDir)),
                () -> assertTrue(Files.exists(googleDir.resolve("timestamp.proto"))),
                () -> assertTrue(Files.exists(googleDir.resolve("duration.proto"))),
                () -> assertTrue(Files.exists(googleDir.resolve("wrappers.proto"))),
                () -> assertTrue(Files.exists(googleDir.resolve("any.proto")))
        );
        assertEquals(tempDir, result);
    }

    @Test
    void resolveProtocolBufferFilesCompiler_windowsPath() {
        System.setProperty("os.name", "Windows 10");
        System.setProperty("os.arch", "x86_64");
        System.setProperty("user.home", "/home/test");

        Path result = ProtocolBuffersUtils.resolveProtocolBufferFilesCompiler();

        String expected = "/home/test/.m2/repository/com/google/protobuf/protoc/4.29.4/protoc-4.29.4-windows-x86_64.exe";
        assertEquals(Path.of(expected), result);
    }

    @Test
    void resolveProtocolBufferFilesCompiler_macAarch64_executable() {
        System.setProperty("os.name", "Mac OS X");
        System.setProperty("os.arch", "aarch64");
        System.setProperty("user.home", "/Users/test");

        Path result = ProtocolBuffersUtils.resolveProtocolBufferFilesCompiler();

        assertTrue(result.toString().contains("osx-aarch_64.exe"));
    }

    @Test
    void resolveProtocolBufferFilesCompiler_linuxArm64_fallsToAarch64() {
        System.setProperty("os.name", "Linux");
        System.setProperty("os.arch", "arm64");
        System.setProperty("user.home", "/home/test");

        Path result = ProtocolBuffersUtils.resolveProtocolBufferFilesCompiler();

        assertTrue(result.toString().contains("linux-aarch_64.exe"));
    }

}