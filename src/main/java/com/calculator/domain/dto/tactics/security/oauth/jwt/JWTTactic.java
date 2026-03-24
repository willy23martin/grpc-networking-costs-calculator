package com.calculator.domain.dto.tactics.security.oauth.jwt;

import com.calculator.domain.dto.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.dto.tactics.security.oauth.OAuthTokenValidationMode;
import com.fasterxml.jackson.annotation.JsonProperty;

import static com.calculator.domain.dto.tactics.security.oauth.jwt.JWTOverhead.JWT_OVERHEAD_BYTES_TYPICAL;

public record JWTTactic(
        @JsonProperty("oauthJwtEnabled")
        boolean oauthJwtEnabled,
        @JsonProperty("tokenValidationMode")
        OAuthTokenValidationMode tokenValidationMode,
        @JsonProperty("tokenTtlSeconds")
        int tokenTtlSeconds,
        @JsonProperty("concurrentClients")
        int concurrentClients,
        @JsonProperty("interceptorType")
        InterceptorType interceptorType
) {

    public static JWTTactic empty() {
        return new JWTTactic(false, OAuthTokenValidationMode.LOCAL, 3600, 1, InterceptorType.UNARY);
    }

    public int effectiveJwtOverheadTypical()  {
        return oauthJwtEnabled ? JWT_OVERHEAD_BYTES_TYPICAL.getOverhead(): 0;
    }

    public long extraRequestsPerSecondFromOAuthTokenAcquisitionCallsToTheAuthorisationServer(long baseRequestsPerSecond) {
        if (!oauthJwtEnabled) return 0;
        int ttl = tokenTtlSeconds > 0 ? tokenTtlSeconds   : 3600;
        int clients = concurrentClients > 0 ? concurrentClients : 1;
        return Math.round((double) baseRequestsPerSecond / ((double) ttl * clients));
    }

    public long extraRequestsPerSecondFromRemoteTokenIntrospection(long baseRequestsPerSecond) {
        return (oauthJwtEnabled && tokenValidationMode == OAuthTokenValidationMode.REMOTE_INTROSPECTION)
                ? baseRequestsPerSecond : 0;
    }
}
