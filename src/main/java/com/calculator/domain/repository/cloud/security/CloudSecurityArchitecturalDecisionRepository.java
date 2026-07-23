package com.calculator.domain.repository.cloud.security;

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

import static com.calculator.infrastructure.cloud.adapters.aws.security.AWSSecurityCostCalculatorAdapter.*;
import static com.calculator.infrastructure.cloud.adapters.aws.security.AWSSecurityWAFCostCalculator.*;

@Component
public class CloudSecurityArchitecturalDecisionRepository {

    private final SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    @Autowired
    public CloudSecurityArchitecturalDecisionRepository(
            SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository
    ) {
        this.securityArchitecturalDecisionRepository = securityArchitecturalDecisionRepository;
    }


    public List<ArchitecturalDecision> getAvailableSecurityDecisions() {
        return new ArrayList<>(
                List.of(
                        getAmazonGuardDutyCloudService(),
                        getAmazonInspectorCloudService(),
                        getAWSWAFCloudService(),
                        getAWSCloudTrailCloudService(),
                        getAmazonMacieCloudService(),
                        getAmazonCloudWatchCloudService(),
                        getAWSAuditManagerCloudService(),
                        getAWSKMSCloudService(),
                        getAWSCertificateManagerCloudService()
                )
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
                                SECURITY_CLOUD_SERVICE_AMAZON_GUARDUTY_FALLBACK_VALUE,
                                "First 500 GB/month free. $" + SECURITY_CLOUD_SERVICE_AMAZON_GUARDUTY_FALLBACK_VALUE + "/GB thereafter (tiered)."
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
                                SECURITY_CLOUD_SERVICE_AMAZON_INSPECTOR_FALLBACK_VALUE,
                                "Amazon EC2 instances scanned per month using SSM-agent based scanning*\t$" + SECURITY_CLOUD_SERVICE_AMAZON_INSPECTOR_FALLBACK_VALUE + "per instance"
                        )
                )
                .supportedArchitecturalTactic(tlsTactic)
                .supportedArchitecturalDecisions(
                        List.of(tlsTactic, mTLSTactic)
                )
                .build();
    }

    public ArchitecturalDecision getAWSCloudTrailCloudService() {
        ArchitecturalTactic oAuthJWTTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getOAuthTactic();

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.INTEGRITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A04:2021", "A09:2021"},
                        new String[]{"Insecure Design", "Security Logging and Monitoring Failures"},
                        new String[]{"CWE-778", "CWE-532", "CWE-288"},
                        "Establishes the foundational [OAuth 2.0 + JWT] control-plane audit trails. Adhering to the <a href=\"https://docs.aws.amazon.com/awscloudtrail/latest/userguide/logging-management-events-with-cloudtrail.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS CloudTrail Governance Specifications</a>, it tracks identity vectors, API structural mutations, and unauthorized token manipulation attempts, providing the underlying visibility required by downstream anomaly engines like GuardDuty."
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
                .id("sec-cloudtrail")
                .name("AWS CloudTrail")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                SECURITY_CLOUD_SERVICE_AMAZON_GUARDUTY_FALLBACK_VALUE,
                                "First 500 GB/month free. $" + SECURITY_CLOUD_SERVICE_AMAZON_GUARDUTY_FALLBACK_VALUE + "/GB thereafter (tiered)."
                        )
                )
                .supportedArchitecturalTactic(oAuthJWTTactic)
                .build();
    }

    public ArchitecturalDecision getAWSWAFCloudService() {
        ArchitecturalTactic mTLSTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getMTLSTactic();
        ArchitecturalTactic tlsTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getTLSTactic();
        CloudService cloudTrailCloudService = (CloudService) getAWSCloudTrailCloudService();

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.INTEGRITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A03:2021","A04:2021"},
                        new String[]{"Injection","Insecure Design"},
                        new String[]{"CWE-89","CWE-79","CWE-20","CWE-400"},
                        "Protects [TLS/mTLS] infrastructure ingress. Aligned with the official <a href=\"https://docs.aws.amazon.com/waf/latest/developerguide/ddos-event-mitigation-logic-continuous-inspection.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Edge Mitigation Framework</a>, this Layer-7 firewall filters web exploits, SQLi, and rate-based DDoS spikes at globally distributed edge locations, shielding backend resources from lower-layer floods and explicit TLS abuse."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.CONFIDENTIALITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A03:2021","A04:2021"},
                        new String[]{"Injection","Insecure Design"},
                        new String[]{"CWE-89","CWE-79","CWE-20","CWE-400"},
                        "Protects [TLS/mTLS] infrastructure ingress. Aligned with the official <a href=\"https://docs.aws.amazon.com/waf/latest/developerguide/ddos-event-mitigation-logic-continuous-inspection.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Edge Mitigation Framework</a>, this Layer-7 firewall filters web exploits, SQLi, and rate-based DDoS spikes at globally distributed edge locations, shielding backend resources from lower-layer floods and explicit TLS abuse."
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
                .id("sec-waf")
                .name("AWS WAF")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                SECURITY_CLOUD_SERVICE_WAF_WEB_ACL_PER_MONTH_VALUE
                                        + SECURITY_CLOUD_SERVICE_WAF_RULE_PER_MONTH_VALUE
                                        + SECURITY_CLOUD_SERVICE_WAF_MILLION_REQUESTS_CHARGES_FALLBACK_VALUE,
                                "WebACL fee " + SECURITY_CLOUD_SERVICE_WAF_WEB_ACL_PER_MONTH_VALUE
                                        + "per-rule " + SECURITY_CLOUD_SERVICE_WAF_RULE_PER_MONTH_VALUE
                                        + "per-million-request charges." + SECURITY_CLOUD_SERVICE_WAF_MILLION_REQUESTS_CHARGES_FALLBACK_VALUE
                        )
                )
                .supportedArchitecturalTactic(tlsTactic)
                .supportedArchitecturalDecisions(
                        List.of(tlsTactic, mTLSTactic, cloudTrailCloudService)
                )
                .build();
    }

    public ArchitecturalDecision getAmazonMacieCloudService() {
        ArchitecturalTactic oAuthJWTTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getOAuthTactic();

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.CONFIDENTIALITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A02:2021"},
                        new String[]{"Cryptographic Failures"},
                        new String[]{"CWE-312","CWE-313","CWE-359"},
                        "Provides automated discovery and classification for data-at-rest. As explicitly outlined in <a href=\"https://docs.aws.amazon.com/wellarchitected/latest/security-pillar/sec_data_classification_auto_classification.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Well-Architected SEC07-BP03</a>, this control handles asynchronous evaluation of data objects stored within Amazon S3. For runtime architectures, incoming live transactions must be written out as object blocks into S3 storage, enabling Macie's automated discovery mechanisms to scan the files and identify sensitive data or personally identifiable information (PII) leaks."
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
                .id("sec-macie")
                .name("Amazon Macie")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                SECURITY_CLOUD_SERVICE_MACIE_FALLBACK_VALUE,
                                "First :1 GB/month free. $" + SECURITY_CLOUD_SERVICE_MACIE_FALLBACK_VALUE + "/GB thereafter."
                        )
                )
                .supportedArchitecturalTactic(oAuthJWTTactic)
                .build();
    }

    public ArchitecturalDecision getAmazonCloudWatchCloudService() {
        ArchitecturalTactic oAuthJWTTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getOAuthTactic();
        ArchitecturalTactic mTLSTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getMTLSTactic();
        ArchitecturalTactic tlsTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getTLSTactic();

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.ACCOUNTABILITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A09:2021"},
                        new String[]{"Security Logging and Monitoring Failures"},
                        new String[]{"CWE-223","CWE-778","CWE-779"},
                        "Provides centralized audit durability. As explicitly directed in Section 2 of the formal <a href=\"https://d1.awsstatic.com/whitepapers/Security/AWS_Security_Checklist.pdf\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Security Checklist Guidelines</a>, this control aggregates application-level and service-level log data stream arrays to a centralized repository, protecting the transactional audit trail from local manipulation or deletion and enabling metric-alarm tracking for operational visibility."
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
                        new String[]{"CWE-223","CWE-778","CWE-779"},
                        "Provides centralized audit durability. As explicitly directed in Section 2 of the formal <a href=\"https://d1.awsstatic.com/whitepapers/Security/AWS_Security_Checklist.pdf\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Security Checklist Guidelines</a>, this control aggregates application-level and service-level log data stream arrays to a centralized repository, protecting the transactional audit trail from local manipulation or deletion and enabling metric-alarm tracking for operational visibility."
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
                .id("sec-cloudwatch")
                .name("Amazon CloudWatch")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                SECURITY_CLOUD_SERVICE_CLOUDWATCH_LOGS_INGESTION_PER_GB_FALLBACK_VALUE
                                + SECURITY_CLOUD_SERVICE_CLOUDWATCH_STORAGE_PER_GB_MONTH_FALLBACK_VALUE,
                                "Collect (Data Ingestion) Standard $" + SECURITY_CLOUD_SERVICE_CLOUDWATCH_LOGS_INGESTION_PER_GB_FALLBACK_VALUE
                                        + " per GB/month Store (Archival) Standard $" + SECURITY_CLOUD_SERVICE_CLOUDWATCH_STORAGE_PER_GB_MONTH_FALLBACK_VALUE
                                        + " per GB compressed"
                        )
                )
                .supportedArchitecturalTactic(oAuthJWTTactic)
                .supportedArchitecturalDecisions(
                        List.of(oAuthJWTTactic, tlsTactic, mTLSTactic)
                )
                .build();
    }

    public ArchitecturalDecision getAWSAuditManagerCloudService() {
        ArchitecturalTactic oAuthJWTTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getOAuthTactic();
        ArchitecturalTactic mTLSTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getMTLSTactic();
        ArchitecturalTactic tlsTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getTLSTactic();

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.ACCOUNTABILITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A09:2021","A05:2021"},
                        new String[]{"Security Logging and Monitoring Failures","Security Misconfiguration"},
                        new String[]{"CWE-778","CWE-693"},
                        "Automates governance assurance tracking. As explicitly detailed in the <a href=\"https://docs.aws.amazon.com/audit-manager/latest/userguide/aws-foundational-security-best-practices.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Audit Manager FSBP Specifications</a>, this governance control automatically aggregates system evidence, resource configuration snapshots, and compliance check results, continually mapping them against prebuilt framework controls to streamline audit readiness and identify posture deviations."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.NON_REPUDIATION.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A09:2021","A05:2021"},
                        new String[]{"Security Logging and Monitoring Failures","Security Misconfiguration"},
                        new String[]{"CWE-778","CWE-693"},
                        "Automates governance assurance tracking. As explicitly detailed in the <a href=\"https://docs.aws.amazon.com/audit-manager/latest/userguide/aws-foundational-security-best-practices.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Audit Manager FSBP Specifications</a>, this governance control automatically aggregates system evidence, resource configuration snapshots, and compliance check results, continually mapping them against prebuilt framework controls to streamline audit readiness and identify posture deviations."
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
                .id("sec-audit")
                .name("AWS Audit Manager")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                SECURITY_CLOUD_SERVICE_AUDIT_MANAGER_FALLBACK_PRICE,
                                "Per 1,000 Audit Manager resource assessments per account per Region $"
                                        + SECURITY_CLOUD_SERVICE_AUDIT_MANAGER_FALLBACK_PRICE
                        )
                )
                .supportedArchitecturalTactic(oAuthJWTTactic)
                .supportedArchitecturalDecisions(
                        List.of(oAuthJWTTactic, tlsTactic, mTLSTactic)
                )
                .build();
    }

    public ArchitecturalDecision getAWSKMSCloudService() {
        ArchitecturalTactic mTLSTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getMTLSTactic();
        ArchitecturalTactic tlsTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getTLSTactic();

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.CONFIDENTIALITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A02:2021"},
                        new String[]{"Cryptographic Failures"},
                        new String[]{"CWE-311","CWE-312","CWE-326","CWE-330"},
                        "Secures cryptographic root material within a tamper-resistant architecture. Operating under strict <a href=\"https://aws.amazon.com/compliance/fips/\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS FIPS 140-3 Level 3 Validation Parameters</a>, this infrastructure component utilizes hardware security modules (HSMs) operating in FIPS-approved modes to generate and isolate customer managed keys, facilitating envelope encryption practices where data keys protect application data-at-rest without exposing the underlying plaintext master key material."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.INTEGRITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A02:2021"},
                        new String[]{"Cryptographic Failures"},
                        new String[]{"CWE-311","CWE-312","CWE-326","CWE-330"},
                        "Secures cryptographic root material within a tamper-resistant architecture. Operating under strict <a href=\"https://aws.amazon.com/compliance/fips/\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS FIPS 140-3 Level 3 Validation Parameters</a>, this infrastructure component utilizes hardware security modules (HSMs) operating in FIPS-approved modes to generate and isolate customer managed keys, facilitating envelope encryption practices where data keys protect application data-at-rest without exposing the underlying plaintext master key material."
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
                .id("sec-kms")
                .name("AWS KMS")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                SECURITY_CLOUD_SERVICE_KMS_CMK_PRICE_PER_MONTH_FALLBACK_VALUE
                                + SECURITY_CLOUD_SERVICE_KMS_API_CALLS_PER_10K_FALLBACK_VALUE,
                                "Each AWS KMS key that you create in AWS KMS costs $" + SECURITY_CLOUD_SERVICE_KMS_CMK_PRICE_PER_MONTH_FALLBACK_VALUE + "/month (prorated hourly)"
                                + "US East (N. Virginia)"
                                + "$" + SECURITY_CLOUD_SERVICE_KMS_API_CALLS_PER_10K_FALLBACK_VALUE + " per 10,000 requests"
                        )
                )
                .supportedArchitecturalTactic(tlsTactic)
                .supportedArchitecturalDecisions(
                        List.of(tlsTactic, mTLSTactic)
                )
                .build();
    }

    public ArchitecturalDecision getAWSCertificateManagerCloudService() {
        ArchitecturalTactic mTLSTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getMTLSTactic();
        ArchitecturalTactic tlsTactic = (ArchitecturalTactic) securityArchitecturalDecisionRepository.getTLSTactic();

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A02:2021"},
                        new String[]{"Cryptographic Failures"},
                        new String[]{"CWE-295", "CWE-311", "CWE-319", "CWE-326"},
                        "Provides automated provisioning, management, and deployment of public and private Transport Layer Security (TLS/SSL) certificates. Operating under strict <a href=\"https://aws.amazon.com/compliance/fips/\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS FIPS 140-3 Validation Parameters</a> endpoints, this service secures data-in-transit by automating the renewal of cryptographic certificates and enforcing strong cipher suites, eliminating vulnerabilities associated with expired, misconfigured, or weak cryptographic bindings across AWS resources."
                )
        );
        qualityTradeoffs.add(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.ORTHOGONAL
                )
        );

        return CloudService.builder()
                .cloudProvider(CloudProvider.AWS)
                .id("sec-acm")
                .name("Certificate Manager")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                0,
                                "By default, ACM issues certificates at no cost for use with services integrated with ACM."
                        )
                )
                .supportedArchitecturalTactic(tlsTactic)
                .supportedArchitecturalDecisions(
                        List.of(tlsTactic, mTLSTactic)
                )
                .build();
    }

}
