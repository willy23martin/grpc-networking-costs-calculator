package com.calculator.infrastructure.cloud.adapters.aws.alb;

import com.calculator.domain.model.architecture.FinOpsStrategy;
import com.calculator.domain.model.cost.InfrastructureCost;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import com.calculator.domain.repository.finops.reliability.FinOpsStrategyReliabilityArchitecturalDecisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSALBCostCalculatorAdapterTest {

    @Mock
    private PricingClient pricingClient;

    @Mock
    private FinOpsStrategyReliabilityArchitecturalDecisionRepository finOpsStrategyReliabilityArchitecturalDecisionRepository;

    @InjectMocks
    private AWSALBCostCalculatorAdapter calculator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(calculator, "albFixedChargePerHour", 0.0225);
        ReflectionTestUtils.setField(calculator, "albLCUFixedChargePerHour", 0.008);
    }

    @Test
    void calculateALBCosts_withMockedPricingApi_returnsValidDataStructure() {
        String rawFixedChargeJson = """
            {
              "product" : {
                "productFamily" : "Load Balancer",
                "attributes" : {
                  "location" : "US East (N. Virginia)",
                  "usagetype" : "USE1-LoadBalancerUsage",
                  "operation" : "LoadBalancing:Application"
                }
              },
              "terms" : {
                "OnDemand" : {
                  "SKU.TERM" : {
                    "priceDimensions" : {
                      "SKU.TERM.DIM" : {
                        "unit" : "Hrs",
                        "pricePerUnit" : { "USD" : "0.0225" }
                      }
                    }
                  }
                }
              }
            }
            """;

        String rawLcuChargeJson = """
            {
              "product" : {
                "productFamily" : "Load Balancer",
                "attributes" : {
                  "location" : "US East (N. Virginia)",
                  "usagetype" : "DataProcessing-Bytes",
                  "operation" : "LoadBalancing:Application"
                }
              },
              "terms" : {
                "OnDemand" : {
                  "SKU.TERM" : {
                    "priceDimensions" : {
                      "SKU.TERM.DIM" : {
                        "unit" : "GB",
                        "pricePerUnit" : { "USD" : "0.008" }
                      }
                    }
                  }
                }
              }
            }
            """;

        when(finOpsStrategyReliabilityArchitecturalDecisionRepository.getFinOpsStrategyForAWSApplicationLoadBalancer())
                .thenReturn(
                        FinOpsStrategy.builder()
                                .id("finops-aws-alb")
                                .name("FinOpsStrategy for ALB")
                                .architecturalCharacteristic(
                                        ArchitecturalCharacteristic.builder()
                                                .name(ArchitecturalCharacteristics.AFFORDABILITY.name())
                                                .build()
                                )
                                .costFactor(
                                        new InfrastructureCost(
                                                """
                                                ALB has NO Reserved Instances or Savings Plans — only usage reduction cuts cost, \n
                                                Consider NLB for pure TCP/UDP: NLCU pricing is often cheaper than ALB LCU at scale.
                                                """
                                        )
                                )
                                .build()
                );

        when(pricingClient.getProducts(any(GetProductsRequest.class)))
                .thenReturn(GetProductsResponse.builder()
                        .priceList(List.of(rawFixedChargeJson, rawLcuChargeJson))
                        .build());

        Map<String, Object> result = calculator.calculateALBCosts();

        System.out.println("====== MOCKITO UNIT TEST OUTPUT MAP ======");
        System.out.println(result);
        System.out.println("==========================================");

        assertThat(result).isNotNull();
        assertThat(result.get("source")).isEqualTo("AWS Pricing API");
        assertThat(result.get("fixedPerHourUsd")).isEqualTo(0.0225);
        assertThat(result.get("lcuPerHourUsd")).isEqualTo(0.008);
        assertThat(result.get("fixedPerMonthUsd")).isEqualTo(16.43);
    }
}