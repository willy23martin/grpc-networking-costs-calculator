package com.calculator.infrastructure.web.rest;

import com.calculator.domain.model.architecture.CloudService;
import com.calculator.domain.model.architecture.cloud.CloudProvider;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.repository.cloud.reliability.CloudReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.resiliency.CloudResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import com.calculator.domain.repository.resiliency.ResiliencyArchitecturalDecisionRepository;
import com.calculator.domain.repository.security.SecurityArchitecturalDecisionRepository;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.Collections;
import java.util.List;

@TestConfiguration
public class TestRepositoryStubsConfiguration {

    @Bean
    @Primary
    public SecurityArchitecturalDecisionRepository securityArchitecturalDecisionRepository() {
        SecurityArchitecturalDecisionRepository repo = Mockito.mock(SecurityArchitecturalDecisionRepository.class);
        Mockito.when(repo.getAvailableSecurityDecisions()).thenReturn(Collections.emptyList());
        return repo;
    }

    @Bean
    @Primary
    public CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository() {
        return Mockito.mock(
                CloudSecurityArchitecturalDecisionRepository.class,
                Mockito.withSettings().defaultAnswer(invocation -> {
                    Class<?> returnType = invocation.getMethod().getReturnType();
                    if (List.class.isAssignableFrom(returnType)) {
                        return Collections.emptyList();
                    }
                    return CloudService.builder()
                            .cloudProvider(CloudProvider.AWS)
                            .id("cloud-service-id")
                            .name("Cloud Service")
                            .architecturalCharacteristic(
                                    ArchitecturalCharacteristic.builder()
                                            .name(ArchitecturalCharacteristics.SECURITY.name())
                                            .qualityTradeoffs(Collections.emptyList()).build()
                            ).costFactor(
                                    new InfrastructureCost(1.0, "Cloud service notes")
                            )
                            .build();
                })
        );
    }

    @Bean
    @Primary
    public ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository() {
        ReliabilityArchitecturalDecisionRepository repo = Mockito.mock(ReliabilityArchitecturalDecisionRepository.class);
        Mockito.when(repo.getAvailableReliabilityDecisions()).thenReturn(Collections.emptyList());
        return repo;
    }

    @Bean
    @Primary
    public CloudReliabilityArchitecturalDecisionRepository cloudReliabilityArchitecturalDecisionRepository() {
        CloudReliabilityArchitecturalDecisionRepository repo = Mockito.mock(CloudReliabilityArchitecturalDecisionRepository.class);
        Mockito.when(repo.getAvailableReliabilityDecisions()).thenReturn(Collections.emptyList());
        return repo;
    }

    @Bean
    @Primary
    public ResiliencyArchitecturalDecisionRepository resiliencyArchitecturalDecisionRepository() {
        ResiliencyArchitecturalDecisionRepository repo = Mockito.mock(ResiliencyArchitecturalDecisionRepository.class);
        Mockito.when(repo.getAvailableResiliencyDecisions()).thenReturn(Collections.emptyList());
        return repo;
    }

    @Bean
    @Primary
    public CloudResiliencyArchitecturalDecisionRepository cloudResiliencyArchitecturalDecisionRepository() {
        CloudResiliencyArchitecturalDecisionRepository repo = Mockito.mock(CloudResiliencyArchitecturalDecisionRepository.class);
        Mockito.when(repo.getAvailableResiliencyDecisions()).thenReturn(Collections.emptyList());
        return repo;
    }
}