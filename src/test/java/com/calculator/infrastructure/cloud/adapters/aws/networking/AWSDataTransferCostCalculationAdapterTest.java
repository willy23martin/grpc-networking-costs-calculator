package com.calculator.infrastructure.cloud.adapters.aws.networking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static com.calculator.infrastructure.cloud.adapters.aws.networking.AWSDataTransferCostCalculationServiceAdapter.AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@SpringBootTest
@TestPropertySource(properties = "aws.pricing.ec2.rates=0.09, 0.085, 0.07, 0.05")
class AWSDataTransferCostCalculationAdapterTest {

    @Autowired
    private AWSDataTransferCostCalculationServiceAdapter service;

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
        final double expected = (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[0] * 0.09) + 0.085;

        assertThat(service.calculateDataTransferCost(10_241.0)).isCloseTo(expected, within(0.01));
    }

    @Test
    void calculateDataTransferCost_SpansAllFourTiers_WhenUsageIsVeryLarge() {
        final double expected = (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[0] * 0.09)
                + (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[1] * 0.085)
                + (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[2] * 0.07)
                + 0.05;

        assertThat(service.calculateDataTransferCost(153_601.0)).isCloseTo(expected, within(0.01));
    }

    @Test
    void calculateDataTransferCost_UsesLastRate_WhenUsageExceedsFinalTierThreshold() {
        final double expected = (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[0] * 0.09)
                + (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[1] * 0.085)
                + (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[2] * 0.07)
                + (100.0 * 0.05);

        assertThat(service.calculateDataTransferCost(153_700.0)).isCloseTo(expected, within(0.01));
    }
}