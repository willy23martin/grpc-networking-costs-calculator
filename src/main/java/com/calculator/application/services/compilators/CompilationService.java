package com.calculator.application.services.compilators;

import com.calculator.domain.dto.results.CompilationResult;
import com.calculator.domain.dto.compilers.ProtocCompiler;
import org.springframework.stereotype.Service;

import javax.tools.JavaCompiler;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class CompilationService {

    private final Path protobufJarPath;

    public CompilationService(Path protobufJarPath) {
        this.protobufJarPath = protobufJarPath;
    }

    public CompilationResult compile(ProtocCompiler protoCompiler, JavaCompiler compiler,
                                     String fullClassNameForRequestMessage) throws IOException, ClassNotFoundException {

        List<String> javaFiles = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(protoCompiler.javaOutDir())) {
            walk.filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> javaFiles.add(p.toString()));
        }

        List<String> compilerArgs = new ArrayList<>();
        compilerArgs.add("-d");
        compilerArgs.add(protoCompiler.javaOutDir().toAbsolutePath().toString());
        compilerArgs.add("-classpath");
        compilerArgs.add(protobufJarPath.toAbsolutePath().toString());
        compilerArgs.addAll(javaFiles);

        int compileResult = compiler.run(null, null, null,
                compilerArgs.toArray(new String[0]));

        URLClassLoader classLoader = new URLClassLoader(
                new URL[]{protoCompiler.javaOutDir().toUri().toURL()},
                this.getClass().getClassLoader()
        );
        classLoader.loadClass(fullClassNameForRequestMessage);
        return new CompilationResult(compileResult, classLoader);
    }
}
