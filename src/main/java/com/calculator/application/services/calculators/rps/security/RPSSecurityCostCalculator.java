package com.calculator.application.services.calculators.rps.security;

import com.calculator.application.services.calculators.rps.RPSNetworkingCostCalculator;
import com.calculator.application.services.calculators.rps.security.jwt.RPSJWTCostCalculator;
import com.calculator.domain.dto.tactics.security.SecurityTactics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RPSSecurityCostCalculator implements RPSNetworkingCostCalculator<SecurityTactics> {

    @Autowired
    private RPSJWTCostCalculator rpsjwtCostCalculator;

    @Override
    public long calculateEffectiveRequestsPerSecond(long baseRequestsPerSecond, SecurityTactics securityTactics) {
        long effectiveRequestsPerSecond = 0L;
        effectiveRequestsPerSecond += securityTactics.tlsTactic().extraRequestsPerSecondFromTLSHandshakesAtTheConfiguredReconnectRate();
        // TODO - Should be refactored to be delegated to the RPSJWTCostCalculator
        effectiveRequestsPerSecond += rpsjwtCostCalculator.extraRequestsPerSecondFromOAuthTokenAcquisitionCallsToTheAuthorisationServer(baseRequestsPerSecond, securityTactics.jwtTactic());
        effectiveRequestsPerSecond += rpsjwtCostCalculator.extraRequestsPerSecondFromRemoteTokenIntrospection(baseRequestsPerSecond, securityTactics.jwtTactic());
        return effectiveRequestsPerSecond;
    }
}
