package com.calculator.application.services.mapper;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.requests.EffectiveRequestPerSecondRequest;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.TimeoutPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.dto.tactics.security.authentication.BasicAuthenticationPattern;
import com.calculator.domain.dto.tactics.security.oauth.jwt.JWTTactic;
import com.calculator.domain.dto.tactics.security.tls.TLSTactic;
import com.calculator.domain.model.architecture.tactics.gRPC.interceptor.InterceptorType;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;

public class ArchitecturalDecisionsDTOMapper {

    public static ArchitecturalDecisionsDTO mapToArchitecturalDecisionsDTO(EffectiveRequestPerSecondRequest effectiveRpsRequest) {
        RetryPattern retryPattern = new RetryPattern(
                effectiveRpsRequest.isRetryEnabled(),
                effectiveRpsRequest.getRetryErrorPercentage()
        );

        InterceptorType interceptorType = InterceptorType.UNARY;
        if(effectiveRpsRequest.getInterceptorType() != null) {
            interceptorType = effectiveRpsRequest.getInterceptorType().equals(InterceptorType.UNARY.name()) ? InterceptorType.UNARY : InterceptorType.STREAM;
        }

        OAuthTokenValidationModes oAuthTokenValidationMode = effectiveRpsRequest.getTokenValidationMode().equals(OAuthTokenValidationModes.LOCAL.name()) ? OAuthTokenValidationModes.LOCAL : OAuthTokenValidationModes.REMOTE_INTROSPECTION;

        SecurityTactics securityTactics = new SecurityTactics(
                new TLSTactic(
                        effectiveRpsRequest.isTlsEnabled(),
                        effectiveRpsRequest.isMtlsEnabled(),
                        effectiveRpsRequest.getTlsReconnectsPerHour()
                ),
                new JWTTactic(
                        effectiveRpsRequest.isOauthEnabled(),
                        oAuthTokenValidationMode,
                        effectiveRpsRequest.getTokenTtlSeconds(),
                        effectiveRpsRequest.getConcurrentClients(),
                        interceptorType
                ),
                new BasicAuthenticationPattern(
                        false
                )
        );

        ArchitecturalDecisionsDTO architecturalDecisions = new ArchitecturalDecisionsDTO(
                effectiveRpsRequest.getBaseRequestPerSecond(),
                ReliabilityTactics.empty(),
                TimeoutPattern.empty(),
                retryPattern,
                CircuitBreakerPattern.empty(),
                securityTactics
        );
        return architecturalDecisions;
    }

}
