package com.calculator.application.services.populator.microservices;

import com.calculator.application.services.mapper.tactics.TacticsMapperService;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.calculator.domain.dto.tactics.microservices.SAGAPatternCostMessages.SAGA_PATTERN_COSTS_ALTER_MESSAGE;
import static com.calculator.infrastructure.web.rest.TCOCalculatorController.DISPLAY_LOCALE;

@Service
public class MicroservicesPattersPopulatorService {

    @Autowired
    TacticsMapperService tacticsMapperService;

    public void populate(ArchitecturalDecisionsDTO architecturalDecisionsDTO, long baseRps,
                         List<Map<String, String>> rpsTactics) {
        int steps = architecturalDecisionsDTO.sagaPattern().sagaCompensatableTransactions() + architecturalDecisionsDTO.sagaPattern().sagaRetriableTransactions() + architecturalDecisionsDTO.sagaPattern().sagaPivotTransactions();
        String sagaConfig = String.format(
                "Steps per SAGA instance — Compensatable: %d | Retriable: %d | Pivot: %d | Total: %d",
                architecturalDecisionsDTO.sagaPattern().sagaCompensatableTransactions(), architecturalDecisionsDTO.sagaPattern().sagaRetriableTransactions(),
                architecturalDecisionsDTO.sagaPattern().sagaPivotTransactions(), steps);
        String impact = steps > 0
                ? "×" + steps + " steps → " + String.format(DISPLAY_LOCALE, "%,d", baseRps * steps) + " req/s"
                : "No steps configured";
        rpsTactics.add(tacticsMapperService.tacticEntry("SAGA Pattern", sagaConfig, SAGA_PATTERN_COSTS_ALTER_MESSAGE.getMessage(), impact));
    }

}
