package com.calculator.application.services.mapper.tradeoffs.reliability;

import com.calculator.domain.dto.tactics.reliability.ReliabilityTradeoffDTO;
import com.calculator.domain.dto.tactics.security.SecurityTradeoffsDTO;
import com.calculator.domain.model.architecture.*;
import com.calculator.domain.repository.ArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReliabilityTradeoffMapperService {

    @Autowired
    ArchitecturalDecisionRepository architecturalDecisionRepository;

    public List<ReliabilityTradeoffDTO> getReliabilityTradeoffs() {
        List<ReliabilityTradeoffDTO> reliabilityMappings = new ArrayList<>();
        List<ArchitecturalDecision> reliabilityDecisions = architecturalDecisionRepository.getAvailableReliabilityDecisions();

        for(var reliabilityDecision: reliabilityDecisions) {
            switch (reliabilityDecision) {
                case ArchitecturalTactic architecturalTactic -> reliabilityMappings.add(mapReliabilityTactic(architecturalTactic));
                case ArchitecturalPattern architecturalPattern -> reliabilityMappings.add(mapReliabilityPattern(architecturalPattern));
                case CloudService cloudService -> reliabilityMappings.add(mapCloudReliabilityService(cloudService));
                case FinOpsStrategy finOpsStrategy -> reliabilityMappings.add(mapFinOpsStrategy(finOpsStrategy));
            }
        }
        return reliabilityMappings;
    }

    private ReliabilityTradeoffDTO mapReliabilityTactic(ArchitecturalTactic architecturalTactic) {
        ReliabilityTradeoffDTO reliabilityTradeoff = ReliabilityTradeoffDTO.builder()
                .tacticId(architecturalTactic.getId())
                .tacticName(architecturalTactic.getName())
                .tacticCategory(architecturalTactic.getArchitecturalCharacteristic().getName())
                .costFactor(architecturalTactic.getCostFactor().getCostFactorNotes())
                .build();
        return reliabilityTradeoff;
    }

    private ReliabilityTradeoffDTO mapReliabilityPattern(ArchitecturalPattern architecturalPattern) {
        ReliabilityTradeoffDTO reliabilityTradeoff = ReliabilityTradeoffDTO.builder()
                .tacticId(architecturalPattern.getId())
                .tacticName(architecturalPattern.getName())
                .tacticCategory(architecturalPattern.getArchitecturalCharacteristic().getName())
                .costFactor(architecturalPattern.getCostFactor().getCostFactorNotes())
                .build();
        return reliabilityTradeoff;
    }

    private ReliabilityTradeoffDTO mapCloudReliabilityService(CloudService cloudService) {
        ReliabilityTradeoffDTO reliabilityTradeoff = ReliabilityTradeoffDTO.builder()
                .tacticId(cloudService.getId())
                .tacticName(cloudService.getName())
                .tacticCategory(cloudService.getArchitecturalCharacteristic().getName())
                .costFactor(cloudService.getCostFactor().getCostFactorNotes())
                .build();
        return reliabilityTradeoff;
    }

    private ReliabilityTradeoffDTO mapFinOpsStrategy(FinOpsStrategy finOpsStrategy) {
        ReliabilityTradeoffDTO reliabilityTradeoff = ReliabilityTradeoffDTO.builder()
                .tacticId(finOpsStrategy.getId())
                .tacticName(finOpsStrategy.getName())
                .tacticCategory(finOpsStrategy.getArchitecturalCharacteristic().getName())
                .costFactor(finOpsStrategy.getCostFactor().getCostFactorNotes())
                .build();
        return reliabilityTradeoff;
    }

}
