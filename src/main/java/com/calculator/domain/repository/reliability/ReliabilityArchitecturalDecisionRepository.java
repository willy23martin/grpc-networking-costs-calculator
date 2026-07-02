package com.calculator.domain.repository.reliability;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
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
public class ReliabilityArchitecturalDecisionRepository {

    public List<ArchitecturalDecision> getAvailableReliabilityDecisions() {
        return List.of(
                getClientSideLoadBalancing(),
                getServerSideLoadBalancing()
        );
    }

    public ArchitecturalDecision getClientSideLoadBalancing() {
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.RELIABILITY.name())
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

        return ArchitecturalTactic.builder()
                .id("tactic-client-lb")
                .name("Client-side Load Balancing")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.RELIABILITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new NetworkingCost(
                                NetworkingCostCriteria.NONE,
                                "Bypasses central proxy overhead with client-side load balancing."
                        )
                )
                .build();
    }

    public ArchitecturalDecision getServerSideLoadBalancing() {
        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.RELIABILITY.name())
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

        return ArchitecturalTactic.builder()
                .id("tactic-server-lb")
                .name("Server-side Load Balancing")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.RELIABILITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                "Adds ALB fixed hourly charge + LCU costs from AWS Pricing API."
                        )
                )
                .build();
    }

}
