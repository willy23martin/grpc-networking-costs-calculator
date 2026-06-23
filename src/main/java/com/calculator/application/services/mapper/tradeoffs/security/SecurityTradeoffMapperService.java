package com.calculator.application.services.mapper.tradeoffs.security;

import com.calculator.domain.dto.tactics.security.SecurityTradeoffsDTO;
import com.calculator.domain.model.architecture.*;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.model.quality.security.SecurityQualityTradeoff;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SecurityTradeoffMapperService {

    @Autowired
    SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    @Autowired
    CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;

    public List<SecurityTradeoffsDTO> getSecurityTradeoffs() {
        List<SecurityTradeoffsDTO> securityMappings = new ArrayList<>();

        List<ArchitecturalDecision> securityDecisions = new ArrayList<>();
        securityDecisions.addAll(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions());
        securityDecisions.addAll(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions());

        for(var securityDecision: securityDecisions) {
            switch (securityDecision) {
                case ArchitecturalTactic architecturalTactic -> securityMappings.add(mapSecurityTactic(architecturalTactic));
                case ArchitecturalPattern architecturalPattern -> securityMappings.add(mapSecurityPattern(architecturalPattern));
                case CloudService cloudService -> securityMappings.add(mapCloudSecurityService(cloudService));
                case FinOpsStrategy finOpsStrategy -> securityMappings.add(mapFinOpsStrategy(finOpsStrategy));
            }
        }
        return securityMappings;
    }

    private SecurityTradeoffsDTO mapSecurityTactic(ArchitecturalTactic architecturalTactic) {
        var securityTradeoff = (SecurityQualityTradeoff)architecturalTactic.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.PROMOTES)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .build(),
                        TradeoffType.PROMOTES)
        );
        var promotedISO25010AttributeTradeoffs = architecturalTactic.getArchitecturalCharacteristic().getQualityTradeoffs().stream()
                .filter(qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.PROMOTES))
                .map(
                qualityTradeoff -> qualityTradeoff.getArchitecturalCharacteristic().getName()
        ).toList();
        var inhibitedCharacteristic = architecturalTactic.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL)
        );

        return SecurityTradeoffsDTO.builder()
                .tacticId(architecturalTactic.getId())
                .tacticName(architecturalTactic.getName())
                .tacticCategory(architecturalTactic.getArchitecturalCharacteristic().getName())
                .owaspTop10(securityTradeoff.getLinkedOwaspTop10Vulnerabilities())
                .owaspLabels(securityTradeoff.getLinkedOwaspLabels())
                .cweIds(securityTradeoff.getCwes())
                .promotedISO25010AttributeTradeoffs(promotedISO25010AttributeTradeoffs)
                .vulnerabilityPrevented(securityTradeoff.getVulnerabilityPrevented())
                .costFactor(architecturalTactic.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .build();
    }

    private SecurityTradeoffsDTO mapSecurityPattern(ArchitecturalPattern architecturalPattern) {
        var securityTradeoff = (SecurityQualityTradeoff)architecturalPattern.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.PROMOTES)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .build(),
                        TradeoffType.PROMOTES)
        );
        var promotedISO25010AttributeTradeoffs = architecturalPattern.getArchitecturalCharacteristic().getQualityTradeoffs().stream()
                .filter(qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.PROMOTES))
                .map(
                qualityTradeoff -> qualityTradeoff.getArchitecturalCharacteristic().getName()
        ).toList();
        var inhibitedCharacteristic = architecturalPattern.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL)
        );
        return SecurityTradeoffsDTO.builder()
                .tacticId(architecturalPattern.getId())
                .tacticName(architecturalPattern.getName())
                .tacticCategory(architecturalPattern.getArchitecturalCharacteristic().getName())
                .owaspTop10(securityTradeoff.getLinkedOwaspTop10Vulnerabilities())
                .owaspLabels(securityTradeoff.getLinkedOwaspLabels())
                .cweIds(securityTradeoff.getCwes())
                .promotedISO25010AttributeTradeoffs(promotedISO25010AttributeTradeoffs)
                .vulnerabilityPrevented(securityTradeoff.getVulnerabilityPrevented())
                .costFactor(architecturalPattern.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .build();
    }

    private SecurityTradeoffsDTO mapCloudSecurityService(CloudService cloudService) {
        var securityTradeoff = (SecurityQualityTradeoff)cloudService.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.PROMOTES)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                    .name(ArchitecturalCharacteristics.SECURITY.name())
                                    .build(),
                        TradeoffType.PROMOTES)
        );
        List<String> supportedArchitecturalDecisions = new ArrayList<>(cloudService.getSupportedArchitecturalDecisions().stream().map(
                ArchitecturalDecision::getId
        ).toList());
        var promotedISO25010AttributeTradeoffs = cloudService.getArchitecturalCharacteristic().getQualityTradeoffs().stream()
                .filter(qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.PROMOTES))
                .map(
                qualityTradeoff -> qualityTradeoff.getArchitecturalCharacteristic().getName()
                )
                .toList();

        var inhibitedCharacteristic = cloudService.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL)
        );
        System.out.println("Cloud service: " + cloudService.getId() + " supported architectural decisions: " + supportedArchitecturalDecisions);

        return SecurityTradeoffsDTO.builder()
                .tacticId(cloudService.getId())
                .tacticName(cloudService.getName())
                .tacticCategory(cloudService.getArchitecturalCharacteristic().getName())
                .owaspTop10(securityTradeoff.getLinkedOwaspTop10Vulnerabilities())
                .owaspLabels(securityTradeoff.getLinkedOwaspLabels())
                .cweIds(securityTradeoff.getCwes())
                .promotedISO25010AttributeTradeoffs(promotedISO25010AttributeTradeoffs)
                .vulnerabilityPrevented(securityTradeoff.getVulnerabilityPrevented())
                .costFactor(cloudService.getCostFactor().getCostFactorNotes())
                .supportedArchitecturalDecisions(supportedArchitecturalDecisions)
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .cloudProvider(cloudService.getCloudProvider().name())
                .build();
    }


    private SecurityTradeoffsDTO mapFinOpsStrategy(FinOpsStrategy finOpsStrategy) {
        var securityTradeoff = (SecurityQualityTradeoff)finOpsStrategy.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.PROMOTES)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .build(),
                        TradeoffType.PROMOTES)
        );
        var promotedISO25010AttributeTradeoffs = finOpsStrategy.getArchitecturalCharacteristic().getQualityTradeoffs().stream()
                .filter(qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.PROMOTES))
                .map(
                        qualityTradeoff -> qualityTradeoff.getArchitecturalCharacteristic().getName()
                )
                .toList();
        var inhibitedCharacteristic = finOpsStrategy.getArchitecturalCharacteristic().getQualityTradeoffs().stream().filter(
                qualityTradeoff -> qualityTradeoff.getTradeoffType().equals(TradeoffType.INHIBITS)
        ).findFirst().orElse(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.PROMOTES)
        );

        return SecurityTradeoffsDTO.builder()
                .tacticId(finOpsStrategy.getId())
                .tacticName(finOpsStrategy.getName())
                .tacticCategory(finOpsStrategy.getArchitecturalCharacteristic().getName())
                .owaspTop10(securityTradeoff.getLinkedOwaspTop10Vulnerabilities())
                .owaspLabels(securityTradeoff.getLinkedOwaspLabels())
                .cweIds(securityTradeoff.getCwes())
                .promotedISO25010AttributeTradeoffs(promotedISO25010AttributeTradeoffs)
                .vulnerabilityPrevented(securityTradeoff.getVulnerabilityPrevented())
                .costFactor(finOpsStrategy.getCostFactor().getCostFactorNotes())
                .impactedAttribute(inhibitedCharacteristic.getArchitecturalCharacteristic().getName())
                .impactType(inhibitedCharacteristic.getTradeoffType().name())
                .build();
    }

}
