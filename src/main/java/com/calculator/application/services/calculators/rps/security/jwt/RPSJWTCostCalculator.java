package com.calculator.application.services.calculators.rps.security.jwt;

import com.calculator.application.services.calculators.rps.RPSNetworkingCostCalculator;
import com.calculator.domain.dto.tactics.security.oauth.jwt.JWTTactic;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import org.springframework.stereotype.Service;

@Service
public class RPSJWTCostCalculator implements RPSNetworkingCostCalculator<JWTTactic> {

    @Override
    public long calculateEffectiveRequestsPerSecond(long baseRequestsPerSecond, JWTTactic jwtTactic) {
        long effectiveRequestsPerSecond = 0L;
        effectiveRequestsPerSecond += extraRequestsPerSecondFromOAuthTokenAcquisitionCallsToTheAuthorisationServer(baseRequestsPerSecond, jwtTactic);
        effectiveRequestsPerSecond += extraRequestsPerSecondFromRemoteTokenIntrospection(baseRequestsPerSecond, jwtTactic);
        return effectiveRequestsPerSecond;
    }

    // TODO - convert to private
    public long extraRequestsPerSecondFromOAuthTokenAcquisitionCallsToTheAuthorisationServer(long baseRequestsPerSecond, JWTTactic jwtTactic) {
        if (!jwtTactic.oauthJwtEnabled()) return 0;
        int ttl = jwtTactic.tokenTtlSeconds() > 0 ? jwtTactic.tokenTtlSeconds() : 3600;
        int clients = jwtTactic.concurrentClients() > 0 ? jwtTactic.concurrentClients() : 1;
        return Math.round((double) baseRequestsPerSecond / ((double) ttl * clients));
    }

    // TODO - convert to private
    public long extraRequestsPerSecondFromRemoteTokenIntrospection(long baseRequestsPerSecond, JWTTactic jwtTactic) {
        if(isOAuthWithJWTAndRemoteIntrospection(jwtTactic)) {
            return baseRequestsPerSecond;
        } else {
            return 0;
        }
    }

    private static boolean isOAuthWithJWTAndRemoteIntrospection(JWTTactic jwtTactic) {
        return jwtTactic.oauthJwtEnabled() && jwtTactic.tokenValidationMode() == OAuthTokenValidationModes.REMOTE_INTROSPECTION;
    }
}
