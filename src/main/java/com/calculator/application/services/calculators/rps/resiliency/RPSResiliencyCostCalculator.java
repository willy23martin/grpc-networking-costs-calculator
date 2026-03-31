package com.calculator.application.services.calculators.rps.resiliency;

import com.calculator.application.services.calculators.rps.RPSNetworkingCostCalculator;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import org.springframework.stereotype.Service;

@Service
public class RPSResiliencyCostCalculator implements RPSNetworkingCostCalculator<RetryPattern> {

    @Override
    public long calculateEffectiveRequestsPerSecond(long baseRequestsPerSecond, RetryPattern pattern) {
        return baseRequestsPerSecond * pattern.tacticRetryTimes();
    }
}
