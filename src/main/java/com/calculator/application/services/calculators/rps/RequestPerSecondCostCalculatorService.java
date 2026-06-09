package com.calculator.application.services.calculators.rps;

import com.calculator.application.services.calculators.rps.resiliency.RPSResiliencyCostCalculator;
import com.calculator.application.services.calculators.rps.security.RPSSecurityCostCalculator;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequestPerSecondCostCalculatorService {

    @Autowired
    RPSResiliencyCostCalculator rpsResiliencyCostCalculator;

    @Autowired
    RPSSecurityCostCalculator rpsSecurityCostCalculator;

    // DESIGN PATTERN: COMPOSED METHOD
    public long calculateEffectiveRequestsPerSecond(ArchitecturalDecisionsDTO architecturalDecisionsDTO) {
        long effectiveRequestsPerSecond = architecturalDecisionsDTO.requestsPerSecond();
        effectiveRequestsPerSecond = calculateForResiliencyPatterns(architecturalDecisionsDTO, effectiveRequestsPerSecond);
        effectiveRequestsPerSecond += rpsSecurityCostCalculator.calculateEffectiveRequestsPerSecond(architecturalDecisionsDTO.requestsPerSecond(), architecturalDecisionsDTO.securityTactics());
        return effectiveRequestsPerSecond;
    }

    private long calculateForResiliencyPatterns(ArchitecturalDecisionsDTO architecturalDecisionsDTO, long effectiveRequestsPerSecond) {
        long addition = 0L;
        if (resiliencyPatternsHaveBeenConfigured(architecturalDecisionsDTO)) {
            addition = rpsResiliencyCostCalculator.calculateEffectiveRequestsPerSecond(architecturalDecisionsDTO.requestsPerSecond(), architecturalDecisionsDTO.retryTactic());
        }
        return effectiveRequestsPerSecond + addition;
    }

    private static boolean resiliencyPatternsHaveBeenConfigured(ArchitecturalDecisionsDTO architecturalDecisionsDTO) {
        return architecturalDecisionsDTO.retryTactic().resiliencyRetryTactic() && architecturalDecisionsDTO.retryTactic().tacticRetryTimes() > 0;
    }

}
