package com.calculator.domain.repository.cloud.reliability;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.model.architecture.CloudService;
import com.calculator.domain.model.architecture.cloud.CloudProvider;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.repository.reliability.ReliabilityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CloudReliabilityArchitecturalDecisionRepository {

    @Autowired
    ReliabilityArchitecturalDecisionRepository reliabilityArchitecturalDecisionRepository;

    public List<ArchitecturalDecision> getAvailableReliabilityDecisions() {
        return new ArrayList<>(
                List.of(
                        getElasticLoadBalancerALBCloudService()
                )
        );
    }

    public ArchitecturalDecision getElasticLoadBalancerALBCloudService() {
        ArchitecturalTactic serverSideLoadBalancing = (ArchitecturalTactic) reliabilityArchitecturalDecisionRepository.getServerSideLoadBalancing();
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

        return CloudService.builder()
                .cloudProvider(CloudProvider.AWS)
                .id("tactic-alb")
                .name("Elastic Load Balancer - ALB Layer 7")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.RELIABILITY.name())
                                .qualityTradeoffs(qualityTradeoffs).build()
                ).costFactor(
                        new InfrastructureCost(
                                "Calculated via <a href=\"https://docs.aws.amazon.com/aws-cost-management/latest/APIReference/API_pricing_GetProducts.html\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">AWS Price List Query API</a> using ServiceCode 'AmazonElasticLoadBalancing' with operations filtered specifically to 'Application' metrics matching the official <a href=\"https://aws.amazon.com/elasticloadbalancing/pricing/\" target=\"_blank\" style=\"color: #7c3aed; text-decoration: underline; font-weight: 600;\">Elastic Load Balancing Pricing Matrix</a>: Standard ALB running instances are billed at $0.0225 per hour, combined with a volumetric usage rate of $0.008 per Load Balancer Capacity Unit (LCU) consumed per hour."// TODO - LOAD FROM AWS
                        )
                )
                .supportedArchitecturalTactic(serverSideLoadBalancing)
                .build();
    }

}
