package com.calculator.domain.repository.cloud.security;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudSecurityArchitecturalDecisionRepositoryTest {

    @Mock
    private SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository;

    @InjectMocks
    private CloudSecurityArchitecturalDecisionRepository repository;

    @Test
    void getAvailableSecurityDecisions_returnsPopulatedList() {
        final ArchitecturalDecision mockTls = ArchitecturalTactic.builder()
                .id("tactic-tls")
                .name("TLS (One-way)")
                .build();

        final ArchitecturalDecision mockMtls = ArchitecturalTactic.builder()
                .id("tactic-mtls")
                .name("mTLS (Mutual TLS)")
                .build();

        final ArchitecturalDecision mockOauth = ArchitecturalTactic.builder()
                .id("tactic-oauth")
                .name("OAuth 2.0 + JWT")
                .build();

        final ArchitecturalDecision mockBasicAuth = ArchitecturalTactic.builder()
                .id("tactic-basic-auth")
                .name("Basic Authentication")
                .build();

        when(securityArchitecturalDecisionRepository.getTLSTactic()).thenReturn(mockTls);
        when(securityArchitecturalDecisionRepository.getMTLSTactic()).thenReturn(mockMtls);
        when(securityArchitecturalDecisionRepository.getOAuthTactic()).thenReturn(mockOauth);
        when(securityArchitecturalDecisionRepository.getBasicAuthTactic()).thenReturn(mockBasicAuth);

        final List<ArchitecturalDecision> decisions = repository.getAvailableSecurityDecisions();

        assertNotNull(decisions);
        assertFalse(decisions.isEmpty(), "The available cloud security decisions list should not be empty");

        final boolean hasKms = decisions.stream()
                .anyMatch(d -> "sec-kms".equals(d.getId()));

        assertTrue(hasKms, "Should contain the AWS KMS encryption decision configuration");
    }
}