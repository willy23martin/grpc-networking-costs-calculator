package com.calculator.domain.model.tactics;

public record ArchitecturalTacticsContext(
        boolean tacticClientLb,
        boolean tacticServerLb,
        boolean tacticTimeout,
        int     tacticTimeoutMs,
        boolean tacticRetry,
        int     tacticRetryTimes,
        boolean tacticCb,
        int     tacticCbMinCalls,
        int     tacticCbHalfOpen,
        int     tacticCbWaitMs,
        int     tacticCbFailureRate,
        boolean tacticSaga,
        int     tacticSagaCompensatable,
        int     tacticSagaRetriable,
        int     tacticSagaPivot
) { }
