package com.calculator.domain.dto.tactics.security.authentication;

public record BasicAuthenticationPattern(
        boolean basicAuthEnabled
) {
    public static BasicAuthenticationPattern empty(){
        return new BasicAuthenticationPattern(false);
    }
}
