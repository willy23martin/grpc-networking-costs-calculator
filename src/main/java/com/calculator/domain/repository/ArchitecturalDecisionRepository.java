package com.calculator.domain.repository;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.repository.cloud.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ArchitecturalDecisionRepository {

    @Autowired
    SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    @Autowired
    CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;

    public List<ArchitecturalDecision> getAvailableSecurityDecisions() {
        List<ArchitecturalDecision> architecturalDecisions = new ArrayList<>();

        architecturalDecisions.addAll(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions());
        architecturalDecisions.addAll(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions());

        return architecturalDecisions;
    }

}
