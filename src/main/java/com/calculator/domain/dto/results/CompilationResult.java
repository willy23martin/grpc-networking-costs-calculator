package com.calculator.domain.dto.results;

import java.net.URLClassLoader;

public record CompilationResult(
        int compilationResult,
        URLClassLoader classLoader
){ }
