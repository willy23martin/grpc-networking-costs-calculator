package com.calculator.domain.dto.tactics.resiliency;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ResiliencyTradeoffDTO {
    @JsonProperty("tacticId")
    private String tacticId;
    @JsonProperty("tacticName")
    public String tacticName;
    @JsonProperty("tacticCategory")
    private String tacticCategory;
    @JsonProperty("costFactor")
    private String costFactor;
    @JsonProperty("impactedAttribute")
    private String impactedAttribute;
    @JsonProperty("impactType")
    private String impactType;
    // Only for Cloud Services
    @JsonProperty("supportedArchitecturalDecisions")
    private List<String> supportedArchitecturalDecisions;
}
