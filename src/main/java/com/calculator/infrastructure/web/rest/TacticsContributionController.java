package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tactics.TacticsContributionService;
import com.calculator.domain.dto.requests.TacticContributionRequest;
import com.calculator.domain.dto.responses.TacticContributionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.calculator.application.services.utils.MathUtils.round2;

@RestController
@RequestMapping("/api/cost")
@CrossOrigin(origins = "*")
public class TacticsContributionController {

    @Autowired
    TacticsContributionService tacticsContributionService;

    @PostMapping("/tactic-contributions")
    public ResponseEntity<TacticContributionResponse> calculateTacticContributions(
            @RequestBody TacticContributionRequest tacticContributionRequest) {

        TacticContributionResponse tacticContributionResponse = new TacticContributionResponse();

        int defaultProtoResponseSizePlaceholderInBytes = 0;
        int responseBytes    = tacticContributionRequest.protoResponseSizeEffectiveBytes > 0
                ? tacticContributionRequest.protoResponseSizeEffectiveBytes
                : defaultProtoResponseSizePlaceholderInBytes;
        tacticContributionResponse.placeholderResponseBytes = defaultProtoResponseSizePlaceholderInBytes;
        tacticContributionResponse.usedPlaceholderBytes = tacticContributionRequest.protoResponseSizeEffectiveBytes <= 0;
        tacticContributionResponse.contributions.addAll(tacticsContributionService.mapStructuralInformationalTacticsWithNoCostImpact(tacticContributionRequest));
        tacticContributionResponse.contributions.addAll(tacticsContributionService.buildRetryContribution(tacticContributionRequest, responseBytes));
        tacticContributionResponse.contributions.addAll(tacticsContributionService.buildTlsContribution(tacticContributionRequest, responseBytes));
        tacticContributionResponse.contributions.addAll(tacticsContributionService.buildOAuthContribution(tacticContributionRequest, responseBytes));

        double total = tacticContributionResponse.contributions.stream()
                .mapToDouble(c -> c.estimatedMonthlyCostUsd)
                .filter(v -> v > 0)
                .sum();
        tacticContributionResponse.totalTacticNetworkingDeltaUsd = round2(total);

        return ResponseEntity.ok(tacticContributionResponse);
    }

}