package com.calculator.domain.dto.tactics.microservices;

public enum SAGAPatternCostMessages {

    SAGA_PATTERN_COSTS_ALTER_MESSAGE(
            """
            Each logical request triggers one SAGA instance. Every step in that instance
            is an independent gRPC call, so actual traffic = base RPS × steps per instance.
            """);

    private String message;

    SAGAPatternCostMessages(String message) {
        this.message = message;
    }

    public String getMessage(){
        return this.message;
    }
}
