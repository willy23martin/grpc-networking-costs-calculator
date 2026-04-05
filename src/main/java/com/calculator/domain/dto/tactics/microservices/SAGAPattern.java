package com.calculator.domain.dto.tactics.microservices;

public record SAGAPattern(
        boolean microservicesSAGAPattern,
        int sagaCompensatableTransactions,
        int sagaRetriableTransactions,
        int sagaPivotTransactions
) {
    public static SAGAPattern empty(){
        return new SAGAPattern(false, 0, 0, 0);
    }
}