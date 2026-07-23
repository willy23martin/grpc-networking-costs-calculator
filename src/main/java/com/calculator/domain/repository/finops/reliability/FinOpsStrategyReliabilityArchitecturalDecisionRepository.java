package com.calculator.domain.repository.finops.reliability;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.architecture.CloudService;
import com.calculator.domain.model.architecture.FinOpsStrategy;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.model.quality.QualityTradeoff;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.domain.repository.cloud.reliability.CloudReliabilityArchitecturalDecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FinOpsStrategyReliabilityArchitecturalDecisionRepository {

    @Autowired
    private CloudReliabilityArchitecturalDecisionRepository cloudReliabilityArchitecturalDecisionRepository;

    public ArchitecturalDecision getFinOpsStrategyForAWSApplicationLoadBalancer() {
        CloudService awsALB =
                (CloudService) cloudReliabilityArchitecturalDecisionRepository.getElasticLoadBalancerALBCloudService();

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
                .id("finops-aws-alb")
                .name("FinOpsStrategy for ALB")
                .architecturalCharacteristic(
                        ArchitecturalCharacteristic.builder()
                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                .qualityTradeoffs(qualityTradeoffs)
                                .build()
                )
                .cloudServices(List.of(awsALB))
                .costFactor(
                        new InfrastructureCost(
                                0,
                                """
                                ALB has NO Reserved Instances or Savings Plans — only usage reduction cuts cost, \n
                                Consider NLB for pure TCP/UDP: NLCU pricing is often cheaper than ALB LCU at scale.
                                """
                        )
                )
                .build();
    }

}
