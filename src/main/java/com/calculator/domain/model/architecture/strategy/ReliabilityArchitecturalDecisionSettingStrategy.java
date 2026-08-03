package com.calculator.domain.model.architecture.strategy;

import com.calculator.domain.dto.tactics.reliability.ReliabilityTactics;
import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReliabilityArchitecturalDecisionSettingStrategy extends  ArchitecturalDecisionSettingStrategy<ReliabilityTactics> {

    @Autowired
    private ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository;

    @Override
    public void setArchitecturalDecisions(
            ReliabilityTactics reliabilityTactics,
            List<ArchitecturalDecision> architecturalDecisionToSetList
    ) {
        if (reliabilityTactics.reliabilityClientSideLoadBalancerTactic()) {
            architecturalDecisionToSetList.add(reliabilityArchitecturalDecisionRepository.getClientSideLoadBalancing());
        }
        if (reliabilityTactics.reliabilityServerSideLoadBalancerTactic()) {
            architecturalDecisionToSetList.add(reliabilityArchitecturalDecisionRepository.getServerSideLoadBalancing());
        }
    }
}
