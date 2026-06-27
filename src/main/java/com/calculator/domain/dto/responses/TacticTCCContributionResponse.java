package com.calculator.domain.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class TacticTCCContributionResponse {
    @JsonProperty
    public List<TacticContributionItem> contributions = new ArrayList<>();
    @JsonProperty
    public double  totalTacticNetworkingDeltaUsd;  // sum of networking cost increases
    @JsonProperty
    public int placeholderResponseBytes;        // what was used when proto not available
    @JsonProperty
    public boolean usedPlaceholderBytes;
}
