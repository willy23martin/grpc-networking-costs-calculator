package com.calculator.infrastructure.cloud.adapters.aws.networking;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.pricing.PricingClient;

import static com.calculator.infrastructure.cloud.adapters.aws.networking.AWSDataTransferCostCalculationServiceAdapter.AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@ExtendWith(SpringExtension.class) // Because it needs to use the lightweight test engine (no Tomcat, no Security)
@ContextConfiguration(classes = {
        AWSDataTransferCostCalculationServiceAdapter.class
})
@TestPropertySource(properties = "aws.pricing.ec2.rates=#{'0.09,0.085,0.07,0.05'.split(',')}")
class AWSDataTransferCostCalculationAdapterTest {

    @MockitoBean
    private PricingClient pricingClient;

    @Autowired
    private AWSDataTransferCostCalculationServiceAdapter awsDataTransferCostCalculationServiceAdapter;

    @Test
    void calculateDataTransferCost_ReturnsZero_WhenResponseGbIsZero() {
        assertThat(awsDataTransferCostCalculationServiceAdapter.calculateDataTransferCost(0.0)).isEqualTo(0.0);
    }

    @ParameterizedTest(name = "{0} GB => ${1}")
    @CsvSource({
            "1.0,     0.09",
            "100.0,   9.0",
            "10240.0, 921.6"
    })
    void calculateDataTransferCost_AppliesFirstTierRate_WhenUsageIsWithinFirstTier(
            double gb, double expectedCost) {
        assertThat(awsDataTransferCostCalculationServiceAdapter.calculateDataTransferCost(gb)).isCloseTo(expectedCost, within(0.01));
    }

    @Test
    void calculateDataTransferCost_SpansMultipleTiers_WhenUsageExceedsFirstTier() {
        double expected = (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[0] * 0.09) + 0.085;

        assertThat(awsDataTransferCostCalculationServiceAdapter.calculateDataTransferCost(10_241.0)).isCloseTo(expected, within(0.01));
    }

    @Test
    void calculateDataTransferCost_SpansAllFourTiers_WhenUsageIsVeryLarge() {
        double expected = (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[0] * 0.09)
                + (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[1] * 0.085)
                + (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[2] * 0.07)
                + 0.05;

        assertThat(awsDataTransferCostCalculationServiceAdapter.calculateDataTransferCost(153_601.0)).isCloseTo(expected, within(0.01));
    }

    @Test
    void calculateDataTransferCost_UsesLastRate_WhenUsageExceedsFinalTierThreshold() {
        double expected = (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[0] * 0.09)
                + (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[1] * 0.085)
                + (AWS_STANDARD_TIER_THRESHOLD_LIMITS_IN_GB[2] * 0.07)
                + (100.0 * 0.05);

        assertThat(awsDataTransferCostCalculationServiceAdapter.calculateDataTransferCost(153_700.0)).isCloseTo(expected, within(0.01));
    }
}