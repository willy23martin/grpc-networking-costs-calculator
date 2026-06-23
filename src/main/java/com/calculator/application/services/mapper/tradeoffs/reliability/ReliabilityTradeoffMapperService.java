package com.calculator.application.services.mapper.tradeoffs.reliability;

import com.calculator.domain.dto.tactics.reliability.ReliabilityTradeoffDTO;
import com.calculator.domain.model.architecture.*;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.repository.cloud.reliability.CloudReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReliabilityTradeoffMapperService {

    @Autowired
    ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository;
    @Autowired
    CloudReliabilityArchitecturalDecisionRepository cloudReliabilityArchitecturalDecisionRepository;

    public List<ReliabilityTradeoffDTO> getReliabilityTradeoffs() {
        List<ReliabilityTradeoffDTO> reliabilityMappings = new ArrayList<>();

        List<ArchitecturalDecision> reliabilityDecisions = new ArrayList<>();
        reliabilityDecisions.addAll(reliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions());
        reliabilityDecisions.addAll(cloudReliabilityArchitecturalDecisionRepository.getAvailableReliabilityDecisions());

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
        var inhibitedCharacteristic = architecturalTactic.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL)
        );

        return ReliabilityTradeoffDTO.builder()
                .tacticId(architecturalTactic.getId())
                .tacticName(architecturalTactic.getName())
                .tacticCategory(architecturalTactic.getArchitecturalCharacteristic().getName())
                .costFactor(architecturalTactic.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .build();
    }

    private ReliabilityTradeoffDTO mapReliabilityPattern(ArchitecturalPattern architecturalPattern) {
        var inhibitedCharacteristic = architecturalPattern.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL)
        );

        return ReliabilityTradeoffDTO.builder()
                .tacticId(architecturalPattern.getId())
                .tacticName(architecturalPattern.getName())
                .tacticCategory(architecturalPattern.getArchitecturalCharacteristic().getName())
                .costFactor(architecturalPattern.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .build();
    }

    private ReliabilityTradeoffDTO mapCloudReliabilityService(CloudService cloudService) {
        var inhibitedCharacteristic = cloudService.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.INHIBITS)
        );

        List<String> supportedArchitecturalDecisions = new ArrayList<>(cloudService.getSupportedArchitecturalDecisions().stream().map(
                ArchitecturalDecision::getId
        ).toList());

        return ReliabilityTradeoffDTO.builder()
                .tacticId(cloudService.getId())
                .tacticName(cloudService.getName())
                .tacticCategory(cloudService.getArchitecturalCharacteristic().getName())
                .costFactor(cloudService.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .supportedArchitecturalDecisions(supportedArchitecturalDecisions)
                .cloudProvider(cloudService.getCloudProvider().name())
                .build();
    }

    private ReliabilityTradeoffDTO mapFinOpsStrategy(FinOpsStrategy finOpsStrategy) {
        var inhibitedCharacteristic = finOpsStrategy.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.PROMOTES)
        );

        return ReliabilityTradeoffDTO.builder()
                .tacticId(finOpsStrategy.getId())
                .tacticName(finOpsStrategy.getName())
                .tacticCategory(finOpsStrategy.getArchitecturalCharacteristic().getName())
                .costFactor(finOpsStrategy.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .build();
    }

}
