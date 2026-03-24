package com.calculator.domain.dto.tactics.security;

import com.calculator.domain.dto.tactics.security.authentication.BasicAuthenticationPattern;
import com.calculator.domain.dto.tactics.security.oauth.jwt.JWTTactic;
import com.calculator.domain.dto.tactics.security.tls.TLSTactic;
import com.fasterxml.jackson.annotation.JsonProperty;

public record SecurityTactics(
        @JsonProperty("tlsTactic")
        TLSTactic tlsTactic,
        @JsonProperty("jwtTactic")
        JWTTactic jwtTactic,
        @JsonProperty("basicAuthenticationPattern")
        BasicAuthenticationPattern basicAuthenticationPattern
) {

    public static SecurityTactics empty() {
        return new SecurityTactics(
                TLSTactic.empty(),
                JWTTactic.empty(),
                BasicAuthenticationPattern.empty()
        );
    }
}