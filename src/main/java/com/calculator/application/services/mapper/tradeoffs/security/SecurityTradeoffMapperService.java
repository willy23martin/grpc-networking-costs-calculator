package com.calculator.application.services.mapper.tradeoffs.security;

import com.calculator.domain.dto.tactics.security.SecurityTradeoffsDTO;
import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.architecture.ArchitecturalPattern;
import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.model.architecture.CloudService;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.model.quality.security.SecurityQualityTradeoff;
import com.calculator.domain.repository.ArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SecurityTradeoffMapperService {

    @Autowired
    private ArchitecturalDecisionRepository architecturalDecisionRepository;

    public List<SecurityTradeoffsDTO> getSecurityTradeoffs() {
        List<SecurityTradeoffsDTO> securityMappings = new ArrayList<>();
        List<ArchitecturalDecision> securityDecisions = architecturalDecisionRepository.getAvailableSecurityDecisions();

        for(var securityDecision: securityDecisions) {
            switch (securityDecision) {
                case ArchitecturalTactic architecturalTactic -> securityMappings.add(mapSecurityTactic(architecturalTactic));
                case ArchitecturalPattern architecturalPattern -> securityMappings.add(mapSecurityPattern(architecturalPattern));
                case CloudService cloudService -> securityMappings.add(mapCloudSecurityService(cloudService));
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

        return SecurityTradeoffsDTO.builder()
                .tacticId(architecturalTactic.getId())
                .tacticName(architecturalTactic.getName())
                .tacticCategory(architecturalTactic.getArchitecturalCharacteristic().getName())
                .owaspTop10(securityTradeoff.getLinkedOwaspTop10Vulnerabilities())
                .owaspLabels(securityTradeoff.getLinkedOwaspLabels())
                .cweIds(securityTradeoff.getCwes())
                .promotedISO25010AttributeTradeoffs(promotedISO25010AttributeTradeoffs)
                .vulnerabilityPrevented(securityTradeoff.getVulnerabilityPrevented())
                .costImpactNote(architecturalTactic.getCostFactor().getCostImpactNotes())
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
        return SecurityTradeoffsDTO.builder()
                .tacticId(architecturalPattern.getId())
                .tacticName(architecturalPattern.getName())
                .tacticCategory(architecturalPattern.getArchitecturalCharacteristic().getName())
                .owaspTop10(securityTradeoff.getLinkedOwaspTop10Vulnerabilities())
                .owaspLabels(securityTradeoff.getLinkedOwaspLabels())
                .cweIds(securityTradeoff.getCwes())
                .promotedISO25010AttributeTradeoffs(promotedISO25010AttributeTradeoffs)
                .vulnerabilityPrevented(securityTradeoff.getVulnerabilityPrevented())
                .costImpactNote(architecturalPattern.getCostFactor().getCostImpactNotes())
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
                .costImpactNote(cloudService.getCostFactor().getCostImpactNotes())
                .supportedArchitecturalDecisions(supportedArchitecturalDecisions)
                .build();
    }

}
