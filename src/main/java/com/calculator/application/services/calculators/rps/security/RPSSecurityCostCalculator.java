package com.calculator.application.services.calculators.rps.security;

import com.calculator.application.services.calculators.rps.RPSNetworkingCostCalculator;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import org.springframework.stereotype.Service;

@Service
public class RPSSecurityCostCalculator implements RPSNetworkingCostCalculator<SecurityTactics> {

    @Override
    public long calculateEffectiveRequestsPerSecond(long baseRequestsPerSecond, SecurityTactics pattern) {
        long effectiveRequestsPerSecond = 0L;
        effectiveRequestsPerSecond += pattern.tlsTactic().extraRequestsPerSecondFromTLSHandshakesAtTheConfiguredReconnectRate();
        effectiveRequestsPerSecond += pattern.jwtTactic().extraRequestsPerSecondFromOAuthTokenAcquisitionCallsToTheAuthorisationServer(baseRequestsPerSecond);
        effectiveRequestsPerSecond += pattern.jwtTactic().extraRequestsPerSecondFromRemoteTokenIntrospection(baseRequestsPerSecond);
        return effectiveRequestsPerSecond;
    }
}
