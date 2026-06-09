package com.calculator.application.services.mapper.tradeoffs.reliability;

import com.calculator.domain.dto.tactics.reliability.ReliabilityTradeoffDTO;

import java.util.ArrayList;
import java.util.List;

public class ReliabilityTradeoffMapperService {

    public List<ReliabilityTradeoffDTO> getSecurityTradeoffs() {
        List<ReliabilityTradeoffDTO> reliabilityMappings = new ArrayList<>();

        reliabilityMappings.add(
                setReliabilityQualityImpact(
                        "tactic-client-lb",
                        "Client-side Load Balancing",
                        "Reliability",
                        "No direct cloud cost impact.")
        );

        reliabilityMappings.add(
                setReliabilityQualityImpact(
                        "tactic-server-lb",
                        "Server-side Load Balancing (ALB)",
                        "Reliability",
                        "Adds ALB fixed hourly charge + LCU costs from AWS Pricing API.")
        );

        return reliabilityMappings;
    }

    private ReliabilityTradeoffDTO setReliabilityQualityImpact(
            String tacticId,
            String tacticName,
            String tacticCategory,
            String costImpactNote) {

        ReliabilityTradeoffDTO reliabilityTradeoff = ReliabilityTradeoffDTO.builder()
                .tacticId(tacticId)
                .tacticName(tacticName)
                .tacticCategory(tacticCategory)
                .costImpactNote(costImpactNote)
                .build();
        return reliabilityTradeoff;
    }

}
