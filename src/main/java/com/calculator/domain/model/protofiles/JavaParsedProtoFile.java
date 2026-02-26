package com.calculator.domain.model.protofiles;

public record JavaParsedProtoFile(
        String javaPackageName,
        String outerClassName,
        String requestMessageSimpleName,
        String responseMessageSimpleName
) { }
