package com.calculator.domain.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FinOpsDiscountRequest {
    @JsonProperty("currentMonthlyContainerCostUsd")
    public double currentMonthlyContainerCostUsd;
    @JsonProperty("riStandard1yr")
    public boolean riStandard1yr;
    @JsonProperty("riStandard3yr")
    public boolean riStandard3yr;
    @JsonProperty("riConvertible1yr")
    public boolean riConvertible1yr;
    @JsonProperty("savingsPlan1yr")
    public boolean savingsPlan1yr;
    @JsonProperty("savingsPlan3yr")
    public boolean savingsPlan3yr;
}
