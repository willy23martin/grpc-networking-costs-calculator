package com.calculator.domain.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public  class FinOpsDiscountResponse {
    @JsonProperty("bestStrategy")
    public String bestStrategy;
    @JsonProperty("maxDiscountPct")
    public double maxDiscountPct;
    @JsonProperty("calculatedMonthlySavingUsd")
    public double calculatedMonthlySavingUsd;
    @JsonProperty("netMonthlyContainerCostUsd")
    public double netMonthlyContainerCostUsd;
}