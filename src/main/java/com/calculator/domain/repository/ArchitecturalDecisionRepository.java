package com.calculator.domain.repository;

import com.calculator.domain.dto.tactics.resiliency.ResiliencyTradeoffDTO;
import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.repository.cloud.reliability.CloudReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.resiliency.CloudResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.resiliency.ResiliencyArchitecturalDecisionRepository;
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

    @Autowired
    ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository;
    @Autowired
    CloudReliabilityArchitecturalDecisionRepository cloudReliabilityArchitecturalDecisionRepository;

    @Autowired
    ResiliencyArchitecturalDecisionRepository resiliencyArchitecturalDecisionRepository;
    @Autowired
    CloudResiliencyArchitecturalDecisionRepository cloudResiliencyArchitecturalDecisionRepository;

    public List<ArchitecturalDecision> getAvailableSecurityDecisions() {
        List<ArchitecturalDecision> architecturalDecisions = new ArrayList<>();

        architecturalDecisions.addAll(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions());
        architecturalDecisions.addAll(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions());

        getAvailableReliabilityDecisions(); // TODO remove from here
        getAvailableResiliencyDecisions(); // TODO remove from here

        return architecturalDecisions;
    }

    public List<ArchitecturalDecision> getAvailableReliabilityDecisions() {
        List<ArchitecturalDecision> architecturalDecisions = new ArrayList<>();

        architecturalDecisions.addAll(reliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions());
        architecturalDecisions.addAll(cloudReliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions());

        return architecturalDecisions;
    }

    public List<ArchitecturalDecision> getAvailableResiliencyDecisions() {
        List<ArchitecturalDecision> architecturalDecisions = new ArrayList<>();

        architecturalDecisions.addAll(resiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions());
        architecturalDecisions.addAll(cloudResiliencyArchitecturalDecisionRepository.getAvailableResiliencyDecisions());

        return architecturalDecisions;
    }

}
