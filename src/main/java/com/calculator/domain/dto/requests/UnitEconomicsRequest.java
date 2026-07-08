package com.calculator.domain.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UnitEconomicsRequest {
    @JsonProperty
    public double egressTransferCostUsd = 0.0;
    @JsonProperty
    public double cloudInfraCostUsd = 0.0;
    @JsonProperty
    public double finopsSavingUsd = 0.0;
    @JsonProperty
    public int effectiveRps = 0;
    @JsonProperty
    public int consumerCount = 1;
    @JsonProperty
    public double revenuePerUserPerMonth = 0.0;
}