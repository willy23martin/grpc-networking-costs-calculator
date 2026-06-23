package com.calculator.domain.dto.requests;

import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TacticContributionRequest {
    @JsonProperty
    public int baseRps = 0;
    @JsonProperty
    public int protoResponseSizeEffectiveBytes = 0;
    @JsonProperty
    public int tlsOverheadBytesFromBackend = 0;
    @JsonProperty
    public int jwtOverheadBytesFromBackend = 0;
    @JsonProperty
    public boolean clientSideLoadBalancingEnabled = false;
    @JsonProperty
    public boolean serverSideLoadBalancingEnabled = false;
    @JsonProperty
    public boolean circuitBreakerEnabled = false;
    @JsonProperty
    public boolean basicAuthEnabled = false;
    @JsonProperty
    public boolean timeoutEnabled = false;
    @JsonProperty
    public int timeoutMs = 0;
    @JsonProperty
    public boolean retryEnabled = false;
    @JsonProperty
    public double retryErrorRatePct = 5.0;
    @JsonProperty
    public boolean tlsEnabled = false;
    @JsonProperty
    public boolean mtlsEnabled = false;
    @JsonProperty
    public int tlsReconnectsPerHour = 0;
    @JsonProperty
    public boolean oauthEnabled = false;
    @JsonProperty
    public String tokenValidationMode = OAuthTokenValidationModes.LOCAL.name();
    @JsonProperty
    public int tokenTtlSeconds = 3600;
    @JsonProperty
    public int concurrentClients = 1;
}
