package com.calculator.cost.cloud.alb.aws;

import com.calculator.application.services.calculators.cost.cloud.alb.aws.alb.AWSALBCostCalculator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AWSALBCostCalculatorTest {

    @Autowired
    private AWSALBCostCalculator calculator;

    @Test
    void calculateALBCosts_executesRealAwsPricingCall_returnsValidDataStructure() {
        // Act - Executes the actual AWS client pull via your active CLI configuration
        Map<String, Object> result = calculator.calculateALBCosts();

        // Print raw result properties cleanly to ensure it parses successfully
        System.out.println("====== LIVE RECOVERY OUTPUT MAP ======");
        System.out.println(result);
        System.out.println("======================================");

        // Assert - Ensures the real call succeeded without defaulting to fallback values
        assertThat(result).isNotNull();
        assertThat(result.get("source")).isEqualTo("AWS Pricing API");

        assertThat(result.get("fixedPerHourUsd")).isInstanceOf(Double.class);
        assertThat((Double) result.get("fixedPerHourUsd")).isGreaterThan(0.0);

        assertThat(result.get("lcuPerHourUsd")).isInstanceOf(Double.class);
        assertThat((Double) result.get("lcuPerHourUsd")).isGreaterThan(0.0);
    }
}