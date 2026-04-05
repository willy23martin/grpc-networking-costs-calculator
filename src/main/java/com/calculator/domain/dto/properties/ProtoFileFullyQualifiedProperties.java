package com.calculator.domain.dto.properties;

public record ProtoFileFullyQualifiedProperties(
        String fullRequestMessageClassName,
        String fullResponseMessageClassName
) { }
