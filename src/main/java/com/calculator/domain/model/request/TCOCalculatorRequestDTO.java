package com.calculator.domain.model.request;

import com.calculator.domain.model.tactics.ArchitecturalTacticsContext;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
public class TCOCalculatorRequestDTO {
    private MultipartFile protoFile;
    private long requestsPerSecond;
    private boolean tacticClientLb;
    private boolean tacticServerLb;
    private boolean tacticTimeout;
    private int tacticTimeoutMs;
    private boolean tacticRetry;
    private int tacticRetryTimes;
    private boolean tacticCb;
    private int tacticCbMinCalls;
    private int tacticCbHalfOpen;
    private int tacticCbWaitMs;
    private int tacticCbFailureRate;
    private boolean tacticSaga;
    private int tacticSagaCompensatable;
    private int tacticSagaRetriable;
    private int tacticSagaPivot;

    public ArchitecturalTacticsContext toTacticsContext() {
        return new ArchitecturalTacticsContext(
                tacticClientLb, tacticServerLb,
                tacticTimeout, tacticTimeoutMs,
                tacticRetry, tacticRetryTimes,
                tacticCb, tacticCbMinCalls, tacticCbHalfOpen, tacticCbWaitMs, tacticCbFailureRate,
                tacticSaga, tacticSagaCompensatable, tacticSagaRetriable, tacticSagaPivot
        );
    }
}
