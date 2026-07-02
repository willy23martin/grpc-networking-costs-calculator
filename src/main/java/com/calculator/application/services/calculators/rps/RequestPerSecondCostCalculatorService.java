package com.calculator.application.services.calculators.rps;

import com.calculator.application.services.calculators.rps.resiliency.RPSResiliencyCostCalculator;
import com.calculator.application.services.calculators.rps.security.RPSSecurityCostCalculator;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class RequestPerSecondCostCalculatorService {

    private final Logger log = Logger.getLogger(RequestPerSecondCostCalculatorService.class.getName());

    @Autowired
    RPSResiliencyCostCalculator rpsResiliencyCostCalculator;

    @Autowired
    RPSSecurityCostCalculator rpsSecurityCostCalculator;

    public long calculateEffectiveRequestsPerSecond(ArchitecturalDecisionsDTO architecturalDecisionsDTO) {
        long effectiveRequestsPerSecond = architecturalDecisionsDTO.requestsPerSecond();

        effectiveRequestsPerSecond += rpsSecurityCostCalculator.calculateEffectiveRequestsPerSecond(
                architecturalDecisionsDTO.requestsPerSecond(),
                architecturalDecisionsDTO.securityTactics()
        );
        effectiveRequestsPerSecond = calculateForResiliencyPatterns(architecturalDecisionsDTO, effectiveRequestsPerSecond);

        log.info("effectiveRequestsPerSecond security: " + effectiveRequestsPerSecond);
        return effectiveRequestsPerSecond;
    }

    private long calculateForResiliencyPatterns(ArchitecturalDecisionsDTO architecturalDecisionsDTO, long currentTrafficBaseline) {
        long addition = 0L;
        if (resiliencyPatternsHaveBeenConfigured(architecturalDecisionsDTO)) {
            addition = rpsResiliencyCostCalculator.calculateEffectiveRequestsPerSecond(
                    currentTrafficBaseline,
                    architecturalDecisionsDTO.retryTactic()
            );
        }
        log.info("effectiveRequestsPerSecond addition from resiliency: " + addition);
        log.info("effectiveRequestsPerSecond rps response: " + currentTrafficBaseline + addition);
        return currentTrafficBaseline + addition;
    }

    private boolean resiliencyPatternsHaveBeenConfigured(ArchitecturalDecisionsDTO architecturalDecisionsDTO) {
        return architecturalDecisionsDTO.retryTactic().resiliencyRetryTactic() && architecturalDecisionsDTO.retryTactic().tacticRetryTimes() > 0;
    }

}
