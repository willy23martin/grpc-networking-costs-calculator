package com.calculator.domain.dto.tactics.security.oauth.jwt;

import com.calculator.domain.model.architecture.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import static com.calculator.domain.model.architecture.tactics.security.JWTOverhead.JWT_OVERHEAD_BYTES_TYPICAL;

@Builder
public record JWTTactic(
        @JsonProperty("oauthJwtEnabled")
        boolean oauthJwtEnabled,
        @JsonProperty("tokenValidationMode")
        OAuthTokenValidationModes tokenValidationMode,
        @JsonProperty("tokenTtlSeconds")
        int tokenTtlSeconds,
        @JsonProperty("concurrentClients")
        int concurrentClients,
        @JsonProperty("interceptorType")
        InterceptorType interceptorType
) {

    public static JWTTactic empty() {
        return new JWTTactic(false, OAuthTokenValidationModes.LOCAL, 3600, 1, InterceptorType.UNARY);
    }

    public int effectiveJwtOverheadTypical()  {
        return oauthJwtEnabled ? JWT_OVERHEAD_BYTES_TYPICAL.getOverhead(): 0;
    }
}
