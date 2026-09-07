package com.calculator.application.services.calculators.rps.resiliency;

import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RPSResiliencyCostCalculatorTest {

    private final RPSResiliencyCostCalculator rpsResiliencyCostCalculator = new RPSResiliencyCostCalculator();

    @Test
    void shouldCalculateEffectiveRpsCorrectly() {
        final RetryPattern pattern = new RetryPattern(true, 50); // 50%

        final long result = rpsResiliencyCostCalculator.calculateEffectiveRequestsPerSecond(200L, pattern);

        assertEquals(100L, result);
    }

    @Test
    void shouldHandleZeroPercentage() {
        final RetryPattern pattern = new RetryPattern(false, 0);

        final long result = rpsResiliencyCostCalculator.calculateEffectiveRequestsPerSecond(500L, pattern);

        assertEquals(0L, result);
    }

    @ParameterizedTest
    @CsvSource({
            "1200, 5, 60",     // Clean division: 1200 * 0.05 = 60.00 -> 60
            "10, 33, 3",       // Rounds down: 10 * 0.33 = 3.30 -> 3
            "10, 37, 4",       // Rounds up: 10 * 0.37 = 3.70 -> 4
            "10, 35, 4",       // Half-up exact midpoint: 10 * 0.35 = 3.50 -> 4
            "100, 0, 0",       // Zero percentage: 100 * 0.00 = 0.00 -> 0
            "0, 50, 0"         // Zero base RPS: 0 * 0.50 = 0.00 -> 0
    })
    void shouldCalculateAndRoundCorrectly(long baseRps, int retryTimes, long expectedRps) {
        final RetryPattern pattern = new RetryPattern(true, retryTimes);

        final long result = rpsResiliencyCostCalculator.calculateEffectiveRequestsPerSecond(baseRps, pattern);

        assertEquals(expectedRps, result);
    }

}
