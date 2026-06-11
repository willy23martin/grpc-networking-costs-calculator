package com.calculator.domain.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EffectiveRequestPerSecondResponse {

    @JsonProperty("baseRps")
    private int baseRps;
    @JsonProperty("effectiveRps")
    private int effectiveRps;
    @JsonProperty("rpsWasAdjusted")
    private boolean rpsWasAdjusted;
    @JsonProperty("breakdown")
    private List<String> breakdown = new ArrayList<>();
    // Per-tactic preview strings (used by the UI to update live previews)
    @JsonProperty("oauthPreview")
    private String oauthPreview;
    @JsonProperty("retryExtra")
    private int retryExtra;
    @JsonProperty("handshakeExtra")
    private int handshakeExtra;
    @JsonProperty("tokenAcqExtra")
    private int tokenAcqExtra;
    @JsonProperty("introspectionExtra")
    private int introspectionExtra;
}
