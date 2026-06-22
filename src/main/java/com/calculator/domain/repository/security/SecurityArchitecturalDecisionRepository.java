package com.calculator.domain.repository.security;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.model.cost.NetworkingCost;
import com.calculator.domain.model.cost.networking.NetworkingCostCriteria;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.model.quality.security.SecurityQualityTradeoff;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SecurityArchitecturalDecisionRepository {

    public List<ArchitecturalDecision> getAvailableSecurityDecisions() {
        return List.of(
                getTLSTactic(),
                getMTLSTactic(),
                getOAuthTactic(),
                getBasicAuthTactic()
        );
    }

    public ArchitecturalDecision getTLSTactic() {
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.CONFIDENTIALITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A02:2021","A07:2021"},
                        new String[]{"Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-319","CWE-523","CWE-326"},
                        "TLS 1.3 (RFC 8446) encrypts the gRPC channel in transit."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.INTEGRITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A02:2021","A07:2021"},
                        new String[]{"Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-319","CWE-523","CWE-326"},
                        "TLS 1.3 (RFC 8446) encrypts the gRPC channel in transit."
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

        return ArchitecturalTactic.builder()
                .id("tactic-tls")
                .name("TLS (One-way)")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new NetworkingCost(
                                NetworkingCostCriteria.OVERHEAD,
                                "Adds ~29 B/frame TLS overhead. Reconnect RPS adds handshake requests."
                        )
                )
                .build();
    }

    public ArchitecturalDecision getMTLSTactic() {
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.CONFIDENTIALITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A01:2021","A02:2021","A07:2021"},
                        new String[]{"Broken Access Control","Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-319","CWE-295","CWE-287","CWE-306"},
                        "mTLS enforces bidirectional certificate authentication."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.INTEGRITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A01:2021","A02:2021","A07:2021"},
                        new String[]{"Broken Access Control","Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-319","CWE-295","CWE-287","CWE-306"},
                        "mTLS enforces bidirectional certificate authentication."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AUTHENTICITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A01:2021","A02:2021","A07:2021"},
                        new String[]{"Broken Access Control","Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-319","CWE-295","CWE-287","CWE-306"},
                        "mTLS enforces bidirectional certificate authentication."
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

        return ArchitecturalTactic.builder()
                .id("tactic-mtls")
                .name("mTLS (Mutual TLS)")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new NetworkingCost(
                                NetworkingCostCriteria.OVERHEAD,
                                "5 TLS messages per handshake vs 2 for one-way TLS. Increases networking cost and processing overhead."
                        )
                )
                .build();
    }

    public ArchitecturalDecision getOAuthTactic() {
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.CONFIDENTIALITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A01:2021","A02:2021","A07:2021"},
                        new String[]{"Broken Access Control","Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-284","CWE-287","CWE-345","CWE-613"},
                        "OAuth 2.0 + JWT enforce fine-grained access control per scope."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.ACCOUNTABILITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A01:2021","A02:2021","A07:2021"},
                        new String[]{"Broken Access Control","Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-284","CWE-287","CWE-345","CWE-613"},
                        "OAuth 2.0 + JWT enforce fine-grained access control per scope."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AUTHENTICITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A01:2021","A02:2021","A07:2021"},
                        new String[]{"Broken Access Control","Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-284","CWE-287","CWE-345","CWE-613"},
                        "OAuth 2.0 + JWT enforce fine-grained access control per scope."
                )
        );
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.NON_REPUDIATION.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A01:2021","A02:2021","A07:2021"},
                        new String[]{"Broken Access Control","Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-284","CWE-287","CWE-345","CWE-613"},
                        "OAuth 2.0 + JWT enforce fine-grained access control per scope."
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

        return ArchitecturalTactic.builder()
                .id("tactic-oauth")
                .name("OAuth 2.0 + JWT")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new NetworkingCost(
                                NetworkingCostCriteria.OVERHEAD,
                                "JWT header overhead ~650 B per request (inbound to service, AWS charges $0). " +
                                        "Token acquisition adds calls to the auth server. TTL tuning is critical."
                        )
                )
                .build();
    }

    public ArchitecturalDecision getBasicAuthTactic() {
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new SecurityQualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.CONFIDENTIALITY.name())
                                .build(),
                        TradeoffType.PROMOTES,
                        new String[]{"A02:2021","A07:2021"},
                        new String[]{"Cryptographic Failures","Identification and Authentication Failures"},
                        new String[]{"CWE-256","CWE-522","CWE-287"},
                        "⚠ NOT RECOMMENDED FOR PRODUCTION. No token rotation or scope-based access control."
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

        return ArchitecturalTactic.builder()
                .id("tactic-basic-auth")
                .name("Basic Authentication")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new NetworkingCost(
                                NetworkingCostCriteria.NONE,
                                "No byte or RPS overhead."
                        )
                )
                .build();
    }


}
