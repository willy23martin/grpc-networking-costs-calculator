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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CloudReliabilityArchitecturalDecisionRepository {

    @Value("${aws.pricing.alb.fixed.charged.per.hour}")
    private double albFixedChargePerHour;

    @Value("${aws.pricing.lcu.fixed.charged.per.hour}")
    private double albLCUFixedChargePerHour;

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
        ArchitecturalTactic serverSideLoadBalancing =
                (ArchitecturalTactic) reliabilityArchitecturalDecisionRepository.getServerSideLoadBalancing();
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
                                albFixedChargePerHour + albLCUFixedChargePerHour,
                                "$" +albFixedChargePerHour + " per GB Data Processed by the LoadBalancer "
                                + "$" + albLCUFixedChargePerHour + " per LoadBalancer-hour (or partial hour)" // Based on the AWS ALB API results /resources/awspricelistapiexamples/aws-alb-pricing.json
                        )
                )
                .supportedArchitecturalTactic(serverSideLoadBalancing)
                .build();
    }

}
