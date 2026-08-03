package com.calculator.application.services.populator.resiliency;

import com.calculator.application.services.calculators.rps.resiliency.RPSResiliencyCostCalculator;
import com.calculator.application.services.mapper.tactics.TacticsMapperService;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import com.calculator.domain.dto.tactics.resiliency.CircuitBreakerPattern;
import com.calculator.domain.dto.tactics.resiliency.retry.RetryPattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.calculator.domain.model.architecture.tactics.resiliency.retry.RetryPatternCostMessages.RETRY_TACTICS_NETWORKING_COST_ALTER_MESSAGE;
import static com.calculator.infrastructure.web.rest.NetworkingCostCalculatorController.DISPLAY_LOCALE;

@Service
public class ResiliencyTacticsPopulatorService {

    @Autowired
    TacticsMapperService tacticsMapperService;

    @Autowired
    RPSResiliencyCostCalculator rpsResiliencyCostCalculator;

    public void populate(
            ArchitecturalDecisionsDTO architecturalDecisionsDTO,
            List<Map<String, String>> rpsTactics,
            List<Map<String, String>> infoTactics
    ) {
        if (architecturalDecisionsDTO.resiliencyPatterns().timeoutPattern().resiliencyTimeoutPattern()) {
            infoTactics.add(tacticsMapperService.tacticEntry("Timeout", architecturalDecisionsDTO.resiliencyPatterns().timeoutPattern().patternTimeoutMilliseconds() + " ms",
                    "Discards lost packets and frees the client thread after the configured wait.", null));
        }
        if (architecturalDecisionsDTO.resiliencyPatterns().retryPattern().resiliencyRetryPattern()) {
            populateRetryPattern(architecturalDecisionsDTO.resiliencyPatterns().retryPattern(), architecturalDecisionsDTO.requestsPerSecond(), rpsTactics);
        }
        if (architecturalDecisionsDTO.resiliencyPatterns().circuitBreakerPattern().resiliencyCircuitBreakerPattern()) {
            populateCircuitBreakerPattern(architecturalDecisionsDTO.resiliencyPatterns().circuitBreakerPattern(), infoTactics);
        }
    }

    private void populateCircuitBreakerPattern(CircuitBreakerPattern circuitBreakerPattern,
                                              List<Map<String, String>> infoTactics) {
        String cbConfig = String.format(
                "Min calls: %d | Half-open calls: %d | Wait: %d ms | Failure threshold: %d%%",
                circuitBreakerPattern.circuitBreakerPatternMinimumCalls(), circuitBreakerPattern.circuitBreakerHalfOpen(),
                circuitBreakerPattern.circuitBreakerWaitMilliseconds(), circuitBreakerPattern.circuitBreakerFailureRate());
        infoTactics.add(tacticsMapperService.tacticEntry("Circuit Breaker", cbConfig,
                "Opens the circuit when failure thresholds are exceeded, protecting downstream services.", null));
    }

    private void populateRetryPattern(RetryPattern retryPattern, long baseRps,
                                     List<Map<String, String>> rpsTactics) {
        long extra = rpsResiliencyCostCalculator.calculateEffectiveRequestsPerSecond(baseRps, retryPattern);
        rpsTactics.add(tacticsMapperService.tacticEntry("Retry", retryPattern.patternRetryTimes() + "% max retries",
                RETRY_TACTICS_NETWORKING_COST_ALTER_MESSAGE.getMessage(),
                "+" + String.format(DISPLAY_LOCALE, "%,d", extra) + " req/s"));
    }

}
