package com.calculator.domain.repository.cloud;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.model.architecture.CloudService;
import com.calculator.domain.model.architecture.cloud.CloudProvider;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.model.quality.security.SecurityQualityTradeoff;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CloudSecurityArchitecturalDecisionRepository {

    @Autowired
    SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    public List<ArchitecturalDecision> getAvailableSecurityDecisions() {
        return List.of(
                getAmazonGuardDutyCloudService(),
                getAmazonInspectorCloudService()
        );
    }

    public ArchitecturalDecision getAmazonGuardDutyCloudService() {
        ArchitecturalTactic oAuthJWTTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getOAuthTactic();
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.ACCOUNTABILITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A09:2021"},
                        new String[]{"Security Logging and Monitoring Failures"},
                        new String[]{"CWE-223","CWE-778"},
                        "Supports [OAuth 2.0 + JWT (RFC 7519)] runtime integrity. Following the <a href=\"https://docs.aws.amazon.com/guardduty/latest/ug/what-is-guardduty.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Security Reference Architecture</a>, its independent ML threat intelligence analyzes VPC Flow Logs to detect anomalous backend data exfiltration patterns or compromised token usage."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.NON_REPUDIATION.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A09:2021"},
                        new String[]{"Security Logging and Monitoring Failures"},
                        new String[]{"CWE-223","CWE-778"},
                        "Supports [OAuth 2.0 + JWT (RFC 7519)] runtime integrity. Following the <a href=\"https://docs.aws.amazon.com/guardduty/latest/ug/what-is-guardduty.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Security Reference Architecture</a>, its independent ML threat intelligence analyzes VPC Flow Logs to detect anomalous backend data exfiltration patterns or compromised token usage."
                )
        );
        qualityTradeoffs.add(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.INHIBITS
                )
        );

        return CloudService.builder()
                .cloudProvider(CloudProvider.AWS)
                .id("sec-guardduty")
                .name("Amazon GuardDuty")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                "Per GB of logs analysed. First 500 GB/month free." // TODO - LOAD FROM AWS
                        )
                )
                .supportedArchitecturalTactic(oAuthJWTTactic)
                .build();
    }

    public ArchitecturalDecision getAmazonInspectorCloudService() {
        ArchitecturalTactic mTLSTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getMTLSTactic();
        ArchitecturalTactic tlsTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getTLSTactic();

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.INTEGRITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A06:2021"},
                        new String[]{"Vulnerable and Outdated Components"},
                        new String[]{"CWE-1104","CWE-937"},
                        "Underpins [TLS/mTLS (RFC 8446)] execution runtimes. Aligned with <a href=\"https://docs.aws.amazon.com/wellarchitected/latest/security-pillar/sec_protect_compute_vulnerability_management.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Well-Architected SEC06-BP01</a>, it continually scans your workload for software vulnerabilities, potential defects, and unintended network exposure (which may include flaws in underlying TLS/mTLS protocol libraries)."
                )
        );
        qualityTradeoffs.add(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.INHIBITS
                )
        );

        return CloudService.builder()
                .cloudProvider(CloudProvider.AWS)
                .id("sec-inspector")
                .name("Amazon Inspector")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                "Per EC2 instance/Container image per month." // TODO - LOAD FROM AWS
                        )
                )
                .supportedArchitecturalTactic(tlsTactic)
                .supportedArchitecturalDecisions(
                        List.of(tlsTactic, mTLSTactic)
                )
                .build();
    }

}
