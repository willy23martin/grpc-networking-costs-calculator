package com.calculator.domain.model.compilers;

import java.nio.file.Path;

public record ProtocCompiler(
        Path javaOutDir,
        Process protoc
) { }
