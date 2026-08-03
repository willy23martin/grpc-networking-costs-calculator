package com.calculator.domain.model.architecture.strategy;

import com.calculator.domain.dto.tactics.security.SecurityTactics;
import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecurityArchitecturalDecisionSettingStrategy extends ArchitecturalDecisionSettingStrategy<SecurityTactics> {

    @Autowired
    private SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    @Override
    public void setArchitecturalDecisions(
            SecurityTactics securityTactics,
            List<ArchitecturalDecision> architecturalDecisionToSetList
    ) {
        if(securityTactics.jwtTactic().oauthJwtEnabled()) {
            architecturalDecisionToSetList.add(securityArchitecturalDecisionRepository.getOAuthTactic());
        }
        if(securityTactics.tlsTactic().tlsEnabled()) {
            architecturalDecisionToSetList.add(securityArchitecturalDecisionRepository.getTLSTactic());
        }
        if(securityTactics.tlsTactic().mtlsEnabled()) {
            architecturalDecisionToSetList.add(securityArchitecturalDecisionRepository.getMTLSTactic());
        }
        if(securityTactics.basicAuthenticationPattern().basicAuthEnabled()) {
            architecturalDecisionToSetList.add(securityArchitecturalDecisionRepository.getBasicAuthTactic());
        }
    }
}
