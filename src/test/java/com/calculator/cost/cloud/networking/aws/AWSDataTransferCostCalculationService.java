package com.calculator.cost.cloud.networking.aws;

import com.calculator.application.services.calculators.cost.cloud.networking.aws.AWSDataTransferCostCalculationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@TestPropertySource(properties = "aws.pricing.ec2.rates")
class AWSDataTransferCostCalculationServiceTest {

    @Autowired
    AWSDataTransferCostCalculationService service;

    @Test
    void calculateDataTransferCost_ReturnsZero_WhenResponseGbIsZero() {
        assertThat(service.calculateDataTransferCost(0.0)).isEqualTo(0.0);
    }

    @ParameterizedTest(name = "{0} GB => ${1}")
    @CsvSource({
            "1.0,     0.09",
            "100.0,   9.0",
            "10240.0, 921.6"
    })
    void calculateDataTransferCost_AppliesFirstTierRate_WhenUsageIsWithinFirstTier(
            double gb, double expectedCost) {
        assertThat(service.calculateDataTransferCost(gb)).isCloseTo(expectedCost, within(0.01));
    }

    @Test
    void calculateDataTransferCost_SpansMultipleTiers_WhenUsageExceedsFirstTier() {
        double expected = (10_240.0 * 0.09) + (1.0 * 0.085);

        assertThat(service.calculateDataTransferCost(10_241.0)).isCloseTo(expected, within(0.01));
    }

    @Test
    void calculateDataTransferCost_SpansAllFourTiers_WhenUsageIsVeryLarge() {
        double expected = (10_240.0 * 0.09)
                + (40_960.0 * 0.085)
                + (102_400.0 * 0.07)
                + (1.0 * 0.05);

        assertThat(service.calculateDataTransferCost(153_601.0)).isCloseTo(expected, within(0.01));
    }

    @Test
    void calculateDataTransferCost_UsesLastRate_WhenUsageExceedsFinalTierThreshold() {
        double expected = (10_240.0 * 0.09)
                + (40_960.0 * 0.085)
                + (102_400.0 * 0.07)
                + (100.0 * 0.05);

        assertThat(service.calculateDataTransferCost(153_700.0)).isCloseTo(expected, within(0.01));
    }
}