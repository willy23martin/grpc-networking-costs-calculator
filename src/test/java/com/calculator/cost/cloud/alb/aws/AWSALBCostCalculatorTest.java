package com.calculator.cost.cloud.alb.aws;

import com.calculator.application.services.calculators.cost.cloud.alb.aws.alb.AWSALBCostCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AWSALBCostCalculatorTest {

    @Mock
    private PricingClient pricingClient;

    @InjectMocks
    private AWSALBCostCalculator calculator;

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
                  "usagetype" : "USE1-LCUUsage",
                  "operation" : "LoadBalancing:Application"
                }
              },
              "terms" : {
                "OnDemand" : {
                  "SKU.TERM" : {
                    "priceDimensions" : {
                      "SKU.TERM.DIM" : {
                        "unit" : "LCU-Hrs",
                        "pricePerUnit" : { "USD" : "0.008" }
                      }
                    }
                  }
                }
              }
            }
            """;

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