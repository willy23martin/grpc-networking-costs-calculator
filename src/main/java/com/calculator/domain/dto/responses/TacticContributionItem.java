package com.calculator.domain.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TacticContributionItem {
    @JsonProperty
    public String  label;
    @JsonProperty
    public String  value;          // optional sub-label, e.g. "5% error rate"
    @JsonProperty
    public String  kind;           // "info" | "rps" | "bytes" | "both"
    @JsonProperty
    public String  detail;         // one-line formula summary for the UI
    @JsonProperty
    public String  note;           // optional warning note
    @JsonProperty
    public int     rpsAdded;
    @JsonProperty
    public int     bytesAdded;
    @JsonProperty
    public boolean jwtOnRequestOnly;
    @JsonProperty
    public double  estimatedMonthlyCostUsd;  // 0 for kind="info"
    @JsonProperty
    public String  costDisplayLabel;         // formatted string for UI
}