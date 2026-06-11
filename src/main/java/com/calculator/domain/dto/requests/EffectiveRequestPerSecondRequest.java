package com.calculator.domain.dto.requests;

import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class EffectiveRequestPerSecondRequest {
    @JsonProperty("baseRps")
    private int baseRequestPerSecond;
    @JsonProperty("retryEnabled")
    private boolean retryEnabled;
    @JsonProperty("retryErrorPct")
    private int retryErrorPercentage;
    @JsonProperty("tlsEnabled")
    private boolean tlsEnabled;
    @JsonProperty("mtlsEnabled")
    private boolean mtlsEnabled;
    @JsonProperty("tlsReconnectsPerHour")
    private int tlsReconnectsPerHour;
    @JsonProperty("oauthEnabled")
    private boolean oauthEnabled;
    @JsonProperty("tokenValidationMode")
    private String  tokenValidationMode;
    @JsonProperty("tokenTtlSeconds")
    private int tokenTtlSeconds;
    @JsonProperty("concurrentClients")
    private int concurrentClients;
    @JsonProperty("interceptorType")
    private String interceptorType;

    public EffectiveRequestPerSecondRequest() {
        this.baseRequestPerSecond = 0;
        this.retryEnabled = false;
        this.retryErrorPercentage = 0;
        this.tlsEnabled = false;
        this.mtlsEnabled = false;
        this.tlsReconnectsPerHour = 0;
        this.oauthEnabled = false;
        this.tokenValidationMode = OAuthTokenValidationModes.NONE.name();
        this.tokenTtlSeconds = 0;
        this.concurrentClients = 1;
    }
}
