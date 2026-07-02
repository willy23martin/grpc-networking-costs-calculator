package com.calculator.domain.dto.tactics.reliability;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReliabilityTradeoffDTO {
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
    @JsonProperty
    private String cloudProvider;
    @JsonProperty("supportedArchitecturalDecisions")
    private List<String> supportedArchitecturalDecisions;
}
