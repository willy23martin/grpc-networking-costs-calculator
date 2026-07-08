package com.calculator.domain.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

public class CloudInfraTotalCostResponse {
    @JsonProperty
    public double grossCloudInfraCostUsd; // sum before FinOps
    @JsonProperty
    public double finopsSavingUsd;
    @JsonProperty
    public double netCloudInfraCostUsd; // after FinOps saving
    @JsonProperty
    public Map<String, Double> perServiceBreakdown = new LinkedHashMap<>();
}
