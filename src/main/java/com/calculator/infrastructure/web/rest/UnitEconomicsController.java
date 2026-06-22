package com.calculator.infrastructure.web.rest;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.repository.ArchitecturalDecisionRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * UnitEconomicsController
 *
 * Two endpoints:
 *
 *   POST /api/cost/cloud-infra-total
 *     Aggregates all cloud service costs from sessionStorage values already
 *     computed by the individual /api/cost/* endpoints, subtracts FinOps
 *     savings, and returns the net cloud infrastructure total.
 *     The frontend reads sessionStorage directly and sends the pre-computed
 *     per-service costs — no re-calculation of individual services here.
 *
 *   POST /api/cost/unit-economics
 *     Receives egress networking cost + cloud infra total + consumer/revenue
 *     context, and computes the full unit economics: TCO, cost per request,
 *     cost per user, ROI, ARPU, break-even, and annual projections.
 */
@RestController
@RequestMapping("/api/cost")
@CrossOrigin(origins = "*")
public class UnitEconomicsController {

    private static final double SECONDS_PER_MONTH = 2_592_000.0;
    private static final int    MONTHS_PER_YEAR   = 12;
    private static final int    DAYS_PER_MONTH    = 30;

    // Add injection below the existing constants:
    @Autowired
    private ArchitecturalDecisionRepository architecturalDecisionRepository;

    /* ================================================================
       POST /api/cost/cloud-infra-total — REQUEST DTO
    ================================================================ */
    public static class CloudInfraTotalRequest {
        /** Costs already computed by the individual /api/cost/* calls */
        @JsonProperty public double albMonthlyCostUsd        = 0.0;
        @JsonProperty public double cacheMonthlyCostUsd      = 0.0;
        @JsonProperty public double databaseMonthlyCostUsd   = 0.0;
        @JsonProperty public double securityMonthlyCostUsd   = 0.0;
        @JsonProperty public double containerMonthlyCostUsd  = 0.0;
        @JsonProperty public double apiGatewayMonthlyCostUsd = 0.0;
        @JsonProperty public double ec2ReplicaMonthlyCostUsd = 0.0;
        /** Best monthly saving from RI / Savings Plan — already computed by /api/cost/finops-discount */
        @JsonProperty public double finopsMonthlySavingUsd   = 0.0;
    }

    /* ================================================================
       POST /api/cost/cloud-infra-total — RESPONSE DTO
    ================================================================ */
    public static class CloudInfraTotalResponse {
        @JsonProperty public double grossCloudInfraCostUsd;   // sum before FinOps
        @JsonProperty public double finopsSavingUsd;
        @JsonProperty public double netCloudInfraCostUsd;     // after FinOps saving
        @JsonProperty public Map<String, Double> perServiceBreakdown = new LinkedHashMap<>();
    }

    /* ================================================================
       ENDPOINT: POST /api/cost/cloud-infra-total
    ================================================================ */
    @PostMapping("/cloud-infra-total")
    public ResponseEntity<CloudInfraTotalResponse> calculateCloudInfraTotal(
            @RequestBody CloudInfraTotalRequest req) {

        CloudInfraTotalResponse resp = new CloudInfraTotalResponse();

        double gross = req.albMonthlyCostUsd
                + req.cacheMonthlyCostUsd
                + req.databaseMonthlyCostUsd
                + req.securityMonthlyCostUsd
                + req.containerMonthlyCostUsd
                + req.apiGatewayMonthlyCostUsd
                + req.ec2ReplicaMonthlyCostUsd;

        double saving = Math.max(0, req.finopsMonthlySavingUsd);

        resp.grossCloudInfraCostUsd = round2(gross);
        resp.finopsSavingUsd        = round2(saving);
        resp.netCloudInfraCostUsd   = round2(Math.max(0, gross - saving));

        if (req.albMonthlyCostUsd        > 0) resp.perServiceBreakdown.put("ALB",           round2(req.albMonthlyCostUsd));
        if (req.cacheMonthlyCostUsd      > 0) resp.perServiceBreakdown.put("ElastiCache",   round2(req.cacheMonthlyCostUsd));
        if (req.databaseMonthlyCostUsd   > 0) resp.perServiceBreakdown.put("Database/Backup", round2(req.databaseMonthlyCostUsd));
        if (req.securityMonthlyCostUsd   > 0) resp.perServiceBreakdown.put("Security",      round2(req.securityMonthlyCostUsd));
        if (req.containerMonthlyCostUsd  > 0) resp.perServiceBreakdown.put("Container",     round2(req.containerMonthlyCostUsd));
        if (req.apiGatewayMonthlyCostUsd > 0) resp.perServiceBreakdown.put("API Gateway",   round2(req.apiGatewayMonthlyCostUsd));
        if (req.ec2ReplicaMonthlyCostUsd > 0) resp.perServiceBreakdown.put("EC2 Replicas",  round2(req.ec2ReplicaMonthlyCostUsd));

        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       POST /api/cost/unit-economics — REQUEST DTO
    ================================================================ */
    public static class UnitEconomicsRequest {
        /** Networking egress cost from /calculateTCO backend render */
        @JsonProperty public double egressTransferCostUsd    = 0.0;
        /** Net cloud infra cost (after FinOps saving) */
        @JsonProperty public double cloudInfraCostUsd        = 0.0;
        /** FinOps saving already subtracted from cloudInfraCostUsd */
        @JsonProperty public double finopsSavingUsd          = 0.0;
        /** Effective RPS (after tactic adjustments) */
        @JsonProperty public int    effectiveRps             = 0;
        /** Number of end users consuming this service */
        @JsonProperty public int    consumerCount            = 1;
        /** Expected monthly revenue per end user (0 = skip ROI analysis) */
        @JsonProperty public double revenuePerUserPerMonth   = 0.0;
    }

    /* ================================================================
       POST /api/cost/unit-economics — RESPONSE DTO
    ================================================================ */
    public static class UnitEconomicsResponse {
        /* ── TCO ── */
        @JsonProperty public double totalMonthlyTcoUsd;
        @JsonProperty public double totalAnnualTcoUsd;
        @JsonProperty public double egressCostUsd;
        @JsonProperty public double cloudInfraCostUsd;
        @JsonProperty public double finopsSavingUsd;

        /* ── Unit costs ── */
        @JsonProperty public double costPerRequestUsd;
        @JsonProperty public double costPerUserPerMonthUsd;
        @JsonProperty public double costPerUserPerDayUsd;
        @JsonProperty public long   totalMonthlyRequests;

        /* ── ROI (only populated when revenuePerUserPerMonth > 0) ── */
        @JsonProperty public boolean hasRevenueData;
        @JsonProperty public double  totalMonthlyRevenueUsd;
        @JsonProperty public double  totalAnnualRevenueUsd;
        @JsonProperty public double  arpuMonthly;
        @JsonProperty public double  monthlyRoiPct;
        @JsonProperty public double  annualRoiPct;
        @JsonProperty public double  netMonthlyProfitUsd;
        @JsonProperty public double  netAnnualProfitUsd;
        @JsonProperty public int     breakEvenUsers;
        @JsonProperty public double  revenuePerDollarInfra;
        @JsonProperty public double  netMarginPerUserMonthly;
        // Add to the existing UnitEconomicsResponse static inner class:
        @JsonProperty public String affordabilityImpact; // "INHIBITS" | "PROMOTES" | "ORTHOGONAL"
    }

    /* ================================================================
       ENDPOINT: POST /api/cost/unit-economics
    ================================================================ */
    @PostMapping("/unit-economics")
    public ResponseEntity<UnitEconomicsResponse> calculateUnitEconomics(
            @RequestBody UnitEconomicsRequest req) {

        // Gather all available architectural decisions across all three
        // characteristics (reliability, resiliency, security) so the
        // affordability trade-off summary can reason across the full set.
        List<ArchitecturalDecision> allDecisions = new ArrayList<>();
        allDecisions.addAll(architecturalDecisionRepository.getAvailableReliabilityDecisions());
        allDecisions.addAll(architecturalDecisionRepository.getAvailableResiliencyDecisions());
        allDecisions.addAll(architecturalDecisionRepository.getAvailableSecurityDecisions());

        com.calculator.domain.model.economics.CostEfficiencyCalculator calculator = new com.calculator.domain.model.economics.CostEfficiencyCalculator(allDecisions)
                .withEgressCost(req.egressTransferCostUsd)
                .withCloudInfraCost(req.cloudInfraCostUsd)
                .withFinOpsSaving(req.finopsSavingUsd)
                .withEffectiveRps(req.effectiveRps)
                .withConsumerCount(req.consumerCount)
                .withRevenuePerUserPerMonth(req.revenuePerUserPerMonth);

        UnitEconomicsResponse resp = calculator.calculateUnitEconomics();
        resp.affordabilityImpact = calculator.summariseAffordabilityImpact().name();

        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       UTILITIES
    ================================================================ */
    private static double round2(double v) { return Math.round(v * 100.0)   / 100.0; }
    private static double round4(double v) { return Math.round(v * 10000.0) / 10000.0; }
    private static double round6(double v) { return Math.round(v * 1000000.0) / 1000000.0; }
}