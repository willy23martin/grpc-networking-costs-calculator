package com.calculator.domain.model.architecture.tactics.resiliency.retry;

public enum RetryPatternCostMessages {

    RETRY_TACTICS_NETWORKING_COST_ALTER_MESSAGE(
            """
            Retries gRPC packet emission under temporary outages. Increases networking costs
            as up to N additional packets may be sent per logical request.
            """);

    private String message;

    RetryPatternCostMessages(String message) {
        this.message = message;
    }

    public String getMessage(){
        return this.message;
    }

}
