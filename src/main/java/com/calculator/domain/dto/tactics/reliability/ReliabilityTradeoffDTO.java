package com.calculator.domain.dto.tactics.reliability;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReliabilityTradeoffDTO {
    @JsonProperty("tacticId")
    private String tacticId;
    @JsonProperty("tacticName")
    public String tacticName;
    @JsonProperty("tacticCategory")
    private String tacticCategory;
    @JsonProperty("costImpactNote")
    private String costImpactNote;
}
