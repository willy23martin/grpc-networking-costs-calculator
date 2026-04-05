package com.calculator.application.services.calculators.rps.microservices;

import com.calculator.application.services.calculators.rps.RPSNetworkingCostCalculator;
import com.calculator.domain.dto.tactics.microservices.SAGAPattern;
import org.springframework.stereotype.Service;

@Service
public class RPSSAGAPatternCostCalculatorRPS implements RPSNetworkingCostCalculator<SAGAPattern> {

    @Override
    public long calculateEffectiveRequestsPerSecond(long baseRequestsPerSecond, SAGAPattern sagaPattern) {
        long additionalRequestsPerSecond = 0L;
        int transactions = sagaPattern.sagaCompensatableTransactions() + sagaPattern.sagaRetriableTransactions() + sagaPattern.sagaPivotTransactions();
        if (transactions > 0) {
            additionalRequestsPerSecond = baseRequestsPerSecond * transactions;
        }
        return additionalRequestsPerSecond;
    }
}
