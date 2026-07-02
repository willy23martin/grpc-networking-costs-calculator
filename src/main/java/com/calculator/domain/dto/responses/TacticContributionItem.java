package com.calculator.domain.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.ToString;

@Builder
@ToString
public class TacticContributionItem {
    @JsonProperty
    @Builder.Default
    public String label = "";

    @JsonProperty
    @Builder.Default
    public String value = "";          // optional sub-label, e.g. "5% error rate"

    @JsonProperty
    @Builder.Default
    public String kind = "info";       // "info" | "rps" | "bytes" | "both"

    @JsonProperty
    public String detail;              // one-line formula summary for the UI

    @JsonProperty
    @Builder.Default
    public String note = "";           // optional warning note

    @JsonProperty
    @Builder.Default
    public int rpsAdded = 0;

    @JsonProperty
    @Builder.Default
    public int bytesAdded = 0;

    @JsonProperty
    public boolean jwtOnRequestOnly;

    @JsonProperty
    @Builder.Default
    public double estimatedMonthlyCostUsd = 0.0;  // 0 for kind="info"

    @JsonProperty
    @Builder.Default
    public String costDisplayLabel = "\u2014";    // formatted string for UI
}