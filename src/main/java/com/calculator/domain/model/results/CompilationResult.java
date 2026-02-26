package com.calculator.domain.model.results;

import java.net.URLClassLoader;

public record CompilationResult(
        int compilationResult,
        URLClassLoader classLoader
){ }
