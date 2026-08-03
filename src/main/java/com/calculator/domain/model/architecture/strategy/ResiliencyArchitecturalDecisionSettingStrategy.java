package com.calculator.domain.model.architecture.strategy;

import com.calculator.domain.dto.tactics.resiliency.ResiliencyPatterns;
import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.repository.resiliency.ResiliencyArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResiliencyArchitecturalDecisionSettingStrategy extends ArchitecturalDecisionSettingStrategy<ResiliencyPatterns> {

    @Autowired
    private ResiliencyArchitecturalDecisionRepository resiliencyArchitecturalDecisionRepository;

    @Override
    public void setArchitecturalDecisions(
            ResiliencyPatterns resiliencyPatterns,
            List<ArchitecturalDecision> architecturalDecisionToSetList
    ) {
        if(resiliencyPatterns.timeoutPattern().resiliencyTimeoutPattern()) {
            architecturalDecisionToSetList.add(resiliencyArchitecturalDecisionRepository.getTimeoutPattern());
        }
        if(resiliencyPatterns.retryPattern().resiliencyRetryPattern()) {
            architecturalDecisionToSetList.add(resiliencyArchitecturalDecisionRepository.getRetryPattern());
        }
        if(resiliencyPatterns.circuitBreakerPattern().resiliencyCircuitBreakerPattern()) {
            architecturalDecisionToSetList.add(resiliencyArchitecturalDecisionRepository.getCircuitBreakerPattern());
        }
    }
}
