package com.calculator.cost.cloud.alb.aws;

import com.calculator.application.services.calculators.cost.cloud.alb.aws.alb.AWSALBCostCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class AWSALBCostCalculatorTest {

    // Minimal valid pricing API JSON for one ALB fixed-charge product
    private static final String FIXED_PRODUCT_JSON = """
        {
          "product": {
            "attributes": {
              "usagetype": "USE1-LoadBalancerUsage"
            }
          },
          "terms": {
            "OnDemand": {
              "term1": {
                "priceDimensions": {
                  "dim1": {
                    "description": "ALB per hour",
                    "beginRange": "0",
                    "endRange": "Inf",
                    "pricePerUnit": { "USD": "0.008" },
                    "unit": "Hrs"
                  }
                }
              }
            }
          }
        }
        """;

    // Minimal valid pricing API JSON for one LCU product
    private static final String LCU_PRODUCT_JSON = """
        {
          "product": {
            "attributes": {
              "usagetype": "USE1-LCUUsage"
            }
          },
          "terms": {
            "OnDemand": {
              "term1": {
                "priceDimensions": {
                  "dim1": {
                    "description": "ALB LCU per hour",
                    "beginRange": "0",
                    "endRange": "Inf",
                    "pricePerUnit": { "USD": "0.008" },
                    "unit": "LCU-Hrs"
                  }
                }
              }
            }
          }
        }
        """;

    private AWSALBCostCalculator calculator;
    private PricingClient mockPricing;

    @BeforeEach
    void setUp() throws Exception {
        calculator  = new AWSALBCostCalculator();
        mockPricing = Mockito.mock(PricingClient.class);

        // Inject mock via reflection — because PricingClient is built in the constructor
        Field pricingField = calculator.getClass()
                .getSuperclass() // AWSCloudCalculator
                .getDeclaredField("pricing");
        pricingField.setAccessible(true);
        pricingField.set(calculator, mockPricing);
    }

    @Test
    void calculateALBCosts_apiReturnsProducts_parsesCorrectly() {
        when(mockPricing.getProducts(any(GetProductsRequest.class)))
                .thenReturn(GetProductsResponse.builder()
                        .priceList(List.of(FIXED_PRODUCT_JSON, LCU_PRODUCT_JSON))
                        .build());

        Map<String, Object> result = calculator.calculateALBCosts();

        assertThat(result.get("fixedPerHourUsd")).isEqualTo(0.008);
        assertThat(result.get("lcuPerHourUsd")).isEqualTo(0.008);
        assertThat(result.get("fixedPerMonthUsd")).isEqualTo(5.84);
        assertThat(result.get("source")).isEqualTo("AWS Pricing API");
    }

    @Test
    void calculateALBCosts_apiEmptyList_usesFallback() {
        when(mockPricing.getProducts(any(GetProductsRequest.class)))
                .thenReturn(GetProductsResponse.builder()
                        .priceList(List.of())   // ← empty, NOT null JSON strings
                        .build());

        Map<String, Object> result = calculator.calculateALBCosts();

        assertThat(result.get("fixedPerHourUsd")).isEqualTo(0.008);
        assertThat(result.get("source")).isEqualTo("fallback");
    }

    @Test
    void calculateALBCosts_apiException_usesFallback() {
        when(mockPricing.getProducts(any(GetProductsRequest.class)))
                .thenThrow(new RuntimeException("API timeout"));

        Map<String, Object> result = calculator.calculateALBCosts();

        assertThat(result.get("source")).isEqualTo("fallback");
        assertThat(result.get("fixedPerMonthUsd")).isEqualTo(5.84);
    }

    @Test
    void calculateALBCosts_monthlyRounding_correctMath() {
        when(mockPricing.getProducts(any(GetProductsRequest.class)))
                .thenReturn(GetProductsResponse.builder()
                        .priceList(List.of(FIXED_PRODUCT_JSON, LCU_PRODUCT_JSON))
                        .build());

        Map<String, Object> result = calculator.calculateALBCosts();

        // 0.008 * 730 = 5.84
        assertThat(result.get("fixedPerMonthUsd")).isEqualTo(5.84);
        assertThat(result.get("lcuPerMonthBase")).isEqualTo(5.84);
    }
}