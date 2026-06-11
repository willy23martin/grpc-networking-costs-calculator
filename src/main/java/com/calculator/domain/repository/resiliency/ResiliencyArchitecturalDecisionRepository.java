package com.calculator.domain.repository.resiliency;

import com.calculator.domain.dto.tactics.resiliency.ResiliencyTradeoffDTO;
import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.architecture.ArchitecturalPattern;
import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.cost.NetworkingCost;
import com.calculator.domain.model.cost.networking.NetworkingCostCriteria;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ResiliencyArchitecturalDecisionRepository {

    public List<ArchitecturalDecision> getAvailableResiliencyDecisions() {
        return List.of(
                getTimeoutPattern(),
                getRetryPattern(),
                getCircuitBreakerPattern()
        );
    }

    public ArchitecturalDecision getTimeoutPattern() {
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.RESILIENCY.name())
                                .build(),
                        TradeoffType.PROMOTES
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

        return ArchitecturalPattern.builder()
                .id("tactic-timeout")
                .name("Timeout")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost("No direct cloud cost impact.")
                )
                .build();
    }

    public ArchitecturalDecision getRetryPattern() {
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.RESILIENCY.name())
                                .build(),
                        TradeoffType.PROMOTES
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

        return ArchitecturalPattern.builder()
                .id("tactic-retry")
                .name("Retry")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new NetworkingCost(
                                NetworkingCostCriteria.REQUESTS_PER_SECOND,
                                "Increases effective RPS proportional to the error rate configured."
                        )
                )
                .build();
    }

    public ArchitecturalDecision getCircuitBreakerPattern() {
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.RESILIENCY.name())
                                .build(),
                        TradeoffType.PROMOTES
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

        return ArchitecturalPattern.builder()
                .id("tactic-cb")
                .name("Circuit Breaker")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.SECURITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost("No direct cloud cost impact.")
                )
                .build();
    }

}
