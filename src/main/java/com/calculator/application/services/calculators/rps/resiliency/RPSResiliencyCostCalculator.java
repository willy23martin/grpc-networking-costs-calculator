package com.calculator.application.services.calculators.rps.resiliency;

import com.calculator.application.services.calculators.rps.RPSNetworkingCostCalculator;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class RPSResiliencyCostCalculator implements RPSNetworkingCostCalculator<RetryPattern> {

    @Override
    public long calculateEffectiveRequestsPerSecond(long baseRequestsPerSecond, RetryPattern pattern) {
        BigDecimal percentage = new BigDecimal(pattern.patternRetryTimes());
        BigDecimal baseRPS = new BigDecimal(baseRequestsPerSecond);
        BigDecimal addition = baseRPS
                .multiply(percentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP); // 100, Because it is a percentage
        return addition.longValue();
    }
}
