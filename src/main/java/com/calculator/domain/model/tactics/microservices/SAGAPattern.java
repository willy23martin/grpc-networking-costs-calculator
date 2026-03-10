package com.calculator.domain.model.tactics.microservices;

public record SAGAPattern(
        boolean microservicesSAGAPattern,
        int sagaCompensatableTransactions,
        int sagaRetriableTransactions,
        int sagaPivotTransactions
) {
}
