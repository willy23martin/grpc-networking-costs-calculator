package com.calculator.application.services.mapper.tradeoffs.resiliency;

import com.calculator.domain.dto.tactics.resiliency.ResiliencyTradeoffDTO;
import com.calculator.domain.model.architecture.*;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
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
        var inhibitedCharacteristic = architecturalTactic.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL)
        );

        return ResiliencyTradeoffDTO.builder()
                .tacticId(architecturalTactic.getId())
                .tacticName(architecturalTactic.getName())
                .tacticCategory(architecturalTactic.getArchitecturalCharacteristic().getName())
                .costFactor(architecturalTactic.getCostFactor().getCostFactorNotes())
                .build();
    }

    private ResiliencyTradeoffDTO mapResiliencyPattern(ArchitecturalPattern architecturalPattern) {
        var inhibitedCharacteristic = architecturalPattern.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL)
        );

        return ResiliencyTradeoffDTO.builder()
                .tacticId(architecturalPattern.getId())
                .tacticName(architecturalPattern.getName())
                .tacticCategory(architecturalPattern.getArchitecturalCharacteristic().getName())
                .costFactor(architecturalPattern.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .build();
    }

    private ResiliencyTradeoffDTO mapCloudResiliencyService(CloudService cloudService) {
        var inhibitedCharacteristic = cloudService.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL)
        );

        return ResiliencyTradeoffDTO.builder()
                .tacticId(cloudService.getId())
                .tacticName(cloudService.getName())
                .tacticCategory(cloudService.getArchitecturalCharacteristic().getName())
                .costFactor(cloudService.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .build();
    }

    private ResiliencyTradeoffDTO mapFinOpsStrategy(FinOpsStrategy finOpsStrategy) {
        var inhibitedCharacteristic = finOpsStrategy.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL)
        );

        return ResiliencyTradeoffDTO.builder()
                .tacticId(finOpsStrategy.getId())
                .tacticName(finOpsStrategy.getName())
                .tacticCategory(finOpsStrategy.getArchitecturalCharacteristic().getName())
                .costFactor(finOpsStrategy.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .build();
    }

}
