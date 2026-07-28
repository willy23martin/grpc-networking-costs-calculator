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
                    architecturalDecisionsDTO.retryPattern()
            );
        }
        long effectiveRequestPerSecond = currentTrafficBaseline + addition;
        log.info("effectiveRequestsPerSecond addition from resiliency: " + addition);
        log.info("effectiveRequestsPerSecond rps response: " + effectiveRequestPerSecond);
        return effectiveRequestPerSecond;
    }

    private boolean resiliencyPatternsHaveBeenConfigured(ArchitecturalDecisionsDTO architecturalDecisionsDTO) {
        return architecturalDecisionsDTO.retryPattern().resiliencyRetryPattern() && architecturalDecisionsDTO.retryPattern().patternRetryTimes() > 0;
    }

}
