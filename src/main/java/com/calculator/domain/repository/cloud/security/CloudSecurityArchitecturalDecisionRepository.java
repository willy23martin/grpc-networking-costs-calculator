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

@Component
public class CloudSecurityArchitecturalDecisionRepository {

    @Autowired
    SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

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
                                "Per GB of logs analysed. First 500 GB/month free." +
                                        "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> using ServiceCode 'AmazonGuardDuty' attributes: VPC/DNS log telemetry processed at $1.00/GB (First 500 GB, with a 30-day foundational free trial period) and CloudTrail audit streams at $4.00 per million events."// TODO - LOAD FROM AWS
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
                                "Per EC2 instance/Container image per month." +
                                        "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> using ServiceCode 'AmazonInspector' variables matching <a href=\"https://aws.amazon.com/inspector/pricing/\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">Amazon Inspector Pricing</a> scales: EC2 host-scans ($1.258/mo), ECR initial-push scans ($0.09), and automated database rescans ($0.01)." // TODO - LOAD FROM AWS
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
                                "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> (ServiceCode 'AWSCloudTrail'): The first copy of management events per region is free. Additional trails or duplicate delivery copies are calculated at $2.00 per 100,000 events, while high-volume data events scale at $0.10 per 100,000 events." // TODO - LOAD FROM AWS
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
                                "WebACL fee + per-rule + per-million-request charges." +
                                        "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> using ServiceCode 'AwsWAF' attributes (WebACL: $5.00/mo, Rule: $1.00/mo, Request: $0.60 per million)."// TODO - LOAD FROM AWS
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
                                "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> using ServiceCode combinations 'AmazonMacie' and 'AmazonS3'. Combines tiered classification scales ($1.00/GB for Macie discovery after free trial thresholds) with baseline storage components (<a href=\"https://aws.amazon.com/s3/pricing/\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">S3 Standard Pricing</a>: $0.023/GB storage overhead + $0.005 per 1,000 PutObject ingestion operations)." // TODO - LOAD FROM AWS
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
                                "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> using ServiceCode 'AmazonCloudWatch' fields mapped to standard <a href=\"https://aws.amazon.com/cloudwatch/pricing/\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">Amazon CloudWatch Pricing</a> tiers: Log collection data ingestion is billed at $0.50 per GB, while subsequent archival storage components accrue at $0.03 per GB-month." // TODO - LOAD FROM AWS
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
                                "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> matching standard <a href=\"https://aws.amazon.com/audit-manager/pricing/\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Audit Manager Pricing</a> metrics: Standard usage is billed strictly on utility at $1.25 per 1,000 resource assessments per account per region (with an introductory free tier providing 35,000 resource assessments per month for the first two months)." // TODO - LOAD FROM AWS
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
                                "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> using ServiceCode 'awskms' metrics matching formal <a href=\"https://aws.amazon.com/kms/pricing/\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS KMS Pricing</a> guidelines: Key storage overhead is flat-rated at $1.00 per Customer Managed Key (CMK) per month (prorated hourly), combined with standard symmetric cryptographic API request processing billed at $0.03 per 10,000 calls (beyond a monthly free tier allowance of 20,000 requests)." // TODO - LOAD FROM AWS
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
                                "$0 no cost" // TODO - LOAD FROM AWS
                        )
                )
                .supportedArchitecturalTactic(tlsTactic)
                .supportedArchitecturalDecisions(
                        List.of(tlsTactic, mTLSTactic)
                )
                .build();
    }

}
