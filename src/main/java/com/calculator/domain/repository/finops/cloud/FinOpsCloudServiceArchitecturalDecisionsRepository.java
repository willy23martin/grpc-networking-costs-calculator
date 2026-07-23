package com.calculator.domain.repository.finops.cloud;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.architecture.CloudService;
import com.calculator.domain.model.architecture.FinOpsStrategy;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.repository.cloud.CloudArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FinOpsCloudServiceArchitecturalDecisionsRepository {

    @Autowired
    private CloudArchitecturalDecisionRepository cloudArchitecturalDecisionRepository;

    public ArchitecturalDecision getFinOpsStrategyForAWSApplicationLoadBalancer() {
        CloudService awsEKS = (CloudService) cloudArchitecturalDecisionRepository.getAmazonEKSControlPlaneCloudService();

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(1);
        qualityTradeoffs.add(
                new QualityTradeoff(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .build(),
                        TradeoffType.PROMOTES
                )
        );

        return FinOpsStrategy.builder()
                .id("cef-spot")
                .name("Spot Instances")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .qualityTradeoffs(qualityTradeoffs)
                                .build()
                )
                .cloudServices(List.of(awsEKS))
                .costFactor(
                        new InfrastructureCost(
                                0, // Becuase it has not been applied in its initialization to any EC2 instance
                                """
                                Up to 90% reduction. Requires interruption-tolerant workloads.
                                """
                        )
                )
                .build();
    }

}
