package com.calculator.domain.repository.cloud.resiliency;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.repository.resiliency.ResiliencyArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CloudResiliencyArchitecturalDecisionRepository {

    @Autowired
    ResiliencyArchitecturalDecisionRepository resiliencyArchitecturalDecisionRepository;

    public List<ArchitecturalDecision> getAvailableResiliencyDecisions() {
        return new ArrayList<>();
    }

}
