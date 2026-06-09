package com.calculator.application.services.mapper.tradeoffs.resiliency;

import com.calculator.domain.dto.tactics.resiliency.ResiliencyTradeoffDTO;

import java.util.ArrayList;
import java.util.List;

public class ResiliencyTradeoffMapperService {

    public List<ResiliencyTradeoffDTO> getSecurityTradeoffs() {
        List<ResiliencyTradeoffDTO> resiliencyMappings = new ArrayList<>();

        resiliencyMappings.add(
                setResiliencyQualityImpact(
                        "tactic-timeout",
                        "Timeout",
                        "Resiliency",
                        "No direct cloud cost impact."
                )
        );

        resiliencyMappings.add(
                setResiliencyQualityImpact(
                        "tactic-retry",
                        "Retry",
                        "Resiliency",
                        "Increases effective RPS proportional to the error rate configured."
                )
        );

        resiliencyMappings.add(
                setResiliencyQualityImpact(
                        "tactic-cb",
                        "Circuit Breaker",
                        "Resiliency",
                        "No direct cloud cost impact."
                )
        );

        return resiliencyMappings;
    }

    private ResiliencyTradeoffDTO setResiliencyQualityImpact(
            String tacticId,
            String tacticName,
            String tacticCategory,
            String costImpactNote) {

        ResiliencyTradeoffDTO reliabilityTradeoff = ResiliencyTradeoffDTO.builder()
                .tacticId(tacticId)
                .tacticName(tacticName)
                .tacticCategory(tacticCategory)
                .costImpactNote(costImpactNote)
                .build();
        return reliabilityTradeoff;
    }

}
