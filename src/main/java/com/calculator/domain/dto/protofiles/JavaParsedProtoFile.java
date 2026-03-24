package com.calculator.domain.dto.protofiles;

public record JavaParsedProtoFile(
        String javaPackageName,
        String outerClassName,
        String requestMessageSimpleName,
        String responseMessageSimpleName
) { }
