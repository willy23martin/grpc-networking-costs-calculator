package com.calculator.domain.repository.cloud;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.architecture.CloudService;
import com.calculator.domain.model.architecture.cloud.CloudProvider;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.calculator.infrastructure.cloud.adapters.aws.containers.AWSContainersCostCalculatorAdapter.FALLBACK_EKS_HOURLY;

@Component
public class CloudArchitecturalDecisionRepository {

    public ArchitecturalDecision getAmazonEKSControlPlaneCloudService() {

        List<QualityTradeoff> qualityTradeoffs = new ArrayList<>(2);
        qualityTradeoffs.add(new QualityTradeoff(
                ArchitecturalCharacteristic.builder()
                        .name(ArchitecturalCharacteristics.RELIABILITY.name())
                        .build(),
                TradeoffType.PROMOTES
        ));
        qualityTradeoffs.add(new QualityTradeoff(
                ArchitecturalCharacteristic.builder()
                        .name(ArchitecturalCharacteristics.AFFORDABILITY.name()).build(),
                TradeoffType.INHIBITS
        ));

        return CloudService.builder()
                .cloudProvider(CloudProvider.AWS)
                .id("service-eks-controlplane")
                .name("AWS EKS")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.RELIABILITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                FALLBACK_EKS_HOURLY,
                                "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> using ServiceCode 'AmazonEKS' with Filters: productFamily='Compute', operation='Cluster', and usageType='BoxUsage' / 'EKS:ClusterContinuous': Standard Kubernetes version support is billed at a flat fee of $0.10 per cluster per hour."
                        )
                )
                .build();
    }

}
