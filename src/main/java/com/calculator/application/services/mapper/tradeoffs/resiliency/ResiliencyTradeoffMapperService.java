package com.calculator.application.services.mapper.tradeoffs.resiliency;

import com.calculator.domain.dto.tactics.resiliency.ResiliencyTradeoffDTO;
import com.calculator.domain.model.architecture.*;
import com.calculator.domain.repository.ArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResiliencyTradeoffMapperService {

    @Autowired
    ArchitecturalDecisionRepository architecturalDecisionRepository;

    public List<ResiliencyTradeoffDTO> getResiliencyTradeoffs() {
        List<ResiliencyTradeoffDTO> resiliencyMappings = new ArrayList<>();
        List<ArchitecturalDecision> resiliencyDecisions = architecturalDecisionRepository.getAvailableResiliencyDecisions();

        for(var resiliencyDecision: resiliencyDecisions) {
            switch (resiliencyDecision) {
                case ArchitecturalTactic architecturalTactic -> resiliencyMappings.add(mapResiliencyTactic(architecturalTactic));
                case ArchitecturalPattern architecturalPattern -> resiliencyMappings.add(mapResiliencyPattern(architecturalPattern));
                case CloudService cloudService -> resiliencyMappings.add(mapCloudResiliencyService(cloudService));
                case FinOpsStrategy finOpsStrategy -> resiliencyMappings.add(mapFinOpsStrategy(finOpsStrategy));
            }
        }
        return resiliencyMappings;
    }

    private ResiliencyTradeoffDTO mapResiliencyTactic(ArchitecturalTactic architecturalTactic) {
        return ResiliencyTradeoffDTO.builder()
                .tacticId(architecturalTactic.getId())
                .tacticName(architecturalTactic.getName())
                .tacticCategory(architecturalTactic.getArchitecturalCharacteristic().getName())
                .costFactor(architecturalTactic.getCostFactor().getCostFactorNotes())
                .build();
    }

    private ResiliencyTradeoffDTO mapResiliencyPattern(ArchitecturalPattern architecturalPattern) {
        return ResiliencyTradeoffDTO.builder()
                .tacticId(architecturalPattern.getId())
                .tacticName(architecturalPattern.getName())
                .tacticCategory(architecturalPattern.getArchitecturalCharacteristic().getName())
                .costFactor(architecturalPattern.getCostFactor().getCostFactorNotes())
                .build();
    }

    private ResiliencyTradeoffDTO mapCloudResiliencyService(CloudService cloudService) {
        return ResiliencyTradeoffDTO.builder()
                .tacticId(cloudService.getId())
                .tacticName(cloudService.getName())
                .tacticCategory(cloudService.getArchitecturalCharacteristic().getName())
                .costFactor(cloudService.getCostFactor().getCostFactorNotes())
                .build();
    }

    private ResiliencyTradeoffDTO mapFinOpsStrategy(FinOpsStrategy finOpsStrategy) {
        return ResiliencyTradeoffDTO.builder()
                .tacticId(finOpsStrategy.getId())
                .tacticName(finOpsStrategy.getName())
                .tacticCategory(finOpsStrategy.getArchitecturalCharacteristic().getName())
                .costFactor(finOpsStrategy.getCostFactor().getCostFactorNotes())
                .build();
    }

}
