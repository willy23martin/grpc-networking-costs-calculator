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
import org.springframework.security.test.context.support.WithMockUser;

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
        // 1. Create mock objects for the foundational security tactics
        // (Basic Auth may not be wrapped inside a Cloud Service, causing the unnecessary stubbing)
        ArchitecturalDecision mockTls = ArchitecturalTactic.builder()
                .id("tactic-tls")
                .name("TLS (One-way)")
                .build();

        ArchitecturalDecision mockMtls = ArchitecturalTactic.builder()
                .id("tactic-mtls")
                .name("mTLS (Mutual TLS)")
                .build();

        ArchitecturalDecision mockOauth = ArchitecturalTactic.builder()
                .id("tactic-oauth")
                .name("OAuth 2.0 + JWT")
                .build();

        ArchitecturalDecision mockBasicAuth = ArchitecturalTactic.builder()
                .id("tactic-basic-auth")
                .name("Basic Authentication")
                .build();

        // 2. Stub out getters safely with lenient behavior enabled
        when(securityArchitecturalDecisionRepository.getTLSTactic()).thenReturn(mockTls);
        when(securityArchitecturalDecisionRepository.getMTLSTactic()).thenReturn(mockMtls);
        when(securityArchitecturalDecisionRepository.getOAuthTactic()).thenReturn(mockOauth);
        when(securityArchitecturalDecisionRepository.getBasicAuthTactic()).thenReturn(mockBasicAuth);

        // 3. Execute the method under test
        List<ArchitecturalDecision> decisions = repository.getAvailableSecurityDecisions();

        // 4. Assert correctness
        assertNotNull(decisions);
        assertFalse(decisions.isEmpty(), "The available cloud security decisions list should not be empty");

        // Verify that the KMS service or built target wrapper exists in the list
        boolean hasKms = decisions.stream()
                .anyMatch(d -> "sec-kms".equals(d.getId()));

        assertTrue(hasKms, "Should contain the AWS KMS encryption decision configuration");
    }
}