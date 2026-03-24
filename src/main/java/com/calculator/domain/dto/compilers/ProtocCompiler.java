package com.calculator.domain.dto.compilers;

import java.nio.file.Path;

public record ProtocCompiler(
        Path javaOutDir,
        Process protoc
) { }
