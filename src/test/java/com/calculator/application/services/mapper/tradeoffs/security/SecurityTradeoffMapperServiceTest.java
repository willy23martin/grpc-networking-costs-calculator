package com.calculator.application.services.mapper.tradeoffs.security;

import com.calculator.domain.dto.tactics.security.SecurityTradeoffsDTO;
import com.calculator.domain.model.architecture.*;
import com.calculator.domain.model.architecture.cloud.CloudProvider;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.cost.NetworkingCost;
import com.calculator.domain.model.cost.networking.NetworkingCostCriteria;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.model.quality.security.SecurityQualityTradeoff;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.calculator.infrastructure.cloud.adapters.aws.networking.AWSDataTransferCostCalculationServiceAdapter.OAUTH_JWT_DATA_TRANSFER_COST;
import static com.calculator.infrastructure.cloud.adapters.aws.security.AWSSecurityCostCalculatorAdapter.SECURITY_CLOUD_SERVICE_KMS_CMK_PRICE_PER_MONTH_FALLBACK_VALUE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityTradeoffMapperServiceTest {

    @Mock
    private SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    @Mock
    private CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;

    @InjectMocks
    private SecurityTradeoffMapperService service;

    @Test
    void getSecurityTradeoffs_emptyRepository_returnsEmptyList() {
        when(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());
        when(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());

        final List<SecurityTradeoffsDTO> results = service.getSecurityTradeoffs();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void getSecurityTradeoffs_withArchitecturalTactic_mapsCorrectly() {
        List<QualityTradeoff> tradeoffs = new ArrayList<>();
        final SecurityQualityTradeoff securityTradeoff = new SecurityQualityTradeoff(
                ArchitecturalCharacteristic.builder().name(ArchitecturalCharacteristics.SECURITY.name()).build(),
                TradeoffType.PROMOTES,
                new String[]{"A01:2021"},
                new String[]{"Broken Access Control"},
                new String[]{"CWE-285"},
                "Prevented unauthorised API invocation context."
        );
        tradeoffs.add(securityTradeoff);

        tradeoffs.add(new QualityTradeoff(
                ArchitecturalCharacteristic.builder().name("CONFIDENTIALITY").build(),
                TradeoffType.PROMOTES
        ));

        final ArchitecturalTactic tactic = ArchitecturalTactic.builder()
                .id("tactic-oauth")
                .name("OAuth 2.0 + JWT")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(tradeoffs)
                                .build()
                )
                .costFactor(
                        new NetworkingCost(NetworkingCostCriteria.OVERHEAD,
                                OAUTH_JWT_DATA_TRANSFER_COST,
                        "Adds 650 Bytes per request header"
                ))
                .build();

        when(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(List.of(tactic));
        when(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());

        final List<SecurityTradeoffsDTO> results = service.getSecurityTradeoffs();
        final SecurityTradeoffsDTO dto = results.getFirst();

        assertNotNull(results);
        assertEquals(1, results.size());

        assertEquals("tactic-oauth", dto.getTacticId());
        assertEquals("OAuth 2.0 + JWT", dto.getTacticName());
        assertEquals("SECURITY", dto.getTacticCategory());
        assertArrayEquals(new String[]{"A01:2021"}, dto.getOwaspTop10());
        assertArrayEquals(new String[]{"Broken Access Control"}, dto.getOwaspLabels());
        assertArrayEquals(new String[]{"CWE-285"}, dto.getCweIds());
        assertEquals("Prevented unauthorised API invocation context.", dto.getVulnerabilityPrevented());
        assertEquals("Adds 650 Bytes per request header", dto.getCostFactor());

        assertTrue(dto.getPromotedISO25010AttributeTradeoffs().contains("CONFIDENTIALITY"));
    }

    @Test
    void getSecurityTradeoffs_withCloudService_mapsCorrectly() {
        // Arrange
        List<QualityTradeoff> tradeoffs = new ArrayList<>();
        final SecurityQualityTradeoff securityTradeoff = new SecurityQualityTradeoff(
                ArchitecturalCharacteristic.builder().name(ArchitecturalCharacteristics.SECURITY.name()).build(),
                TradeoffType.PROMOTES,
                new String[]{"A05:2021"},
                new String[]{"Security Misconfiguration"},
                new String[]{"CWE-311"},
                "Encrypts data at rest via automatic key rotation."
        );
        tradeoffs.add(securityTradeoff);

        final CloudService cloudService = CloudService.builder()
                .id("sec-kms")
                .name("AWS KMS (Encryption)")
                .cloudProvider(CloudProvider.AWS)
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(tradeoffs)
                                .build()
                )
                .costFactor(
                        new InfrastructureCost(
                                SECURITY_CLOUD_SERVICE_KMS_CMK_PRICE_PER_MONTH_FALLBACK_VALUE,
                                "Flat rate of $" + SECURITY_CLOUD_SERVICE_KMS_CMK_PRICE_PER_MONTH_FALLBACK_VALUE + " per month per key")
                )
                .build();

        when(securityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());
        when(cloudSecurityArchitecturalDecisionRepository.getAvailableSecurityDecisions()).thenReturn(List.of(cloudService));


        final List<SecurityTradeoffsDTO> results = service.getSecurityTradeoffs();
        final SecurityTradeoffsDTO dto = results.getFirst();

        assertNotNull(results);
        assertEquals(1, results.size());

        assertEquals("sec-kms", dto.getTacticId());
        assertEquals("AWS KMS (Encryption)", dto.getTacticName());
        assertEquals("Flat rate of $1.0 per month per key", dto.getCostFactor());
        assertEquals("Encrypts data at rest via automatic key rotation.", dto.getVulnerabilityPrevented());
    }
}