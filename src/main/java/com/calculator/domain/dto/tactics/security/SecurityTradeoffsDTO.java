package com.calculator.domain.dto.tactics.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SecurityTradeoffsDTO {
    @JsonProperty("tacticId")
    private String tacticId;
    @JsonProperty("tacticName")
    public String tacticName;
    @JsonProperty("tacticCategory")
    private String tacticCategory;
    @JsonProperty("owaspTop10")
    private String[] owaspTop10;
    @JsonProperty("owaspLabels")
    private String[] owaspLabels;
    @JsonProperty("cweIds")
    public String[] cweIds; // https://cwe.mitre.org/
    @JsonProperty("iso25010Attributes")
    List<String> promotedISO25010AttributeTradeoffs;
    @JsonProperty("vulnerabilityPrevented")
    private String vulnerabilityPrevented;
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
