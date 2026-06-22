package com.calculator.domain.model.economics;

import com.calculator.domain.model.architecture.ArchitecturalDecision;
import com.calculator.domain.model.quality.TradeoffType;
import com.calculator.infrastructure.web.rest.UnitEconomicsController.UnitEconomicsResponse;

import java.util.List;

/**
 * CostEfficiencyCalculator
 *
 * Domain calculator that combines a set of ArchitecturalDecisions with
 * service cost inputs and computes the full UnitEconomicsResponse.
 *
 * The key cost-efficiency signal is the comparison of Average Revenue Per User
 * (ARPU) against the cost per user per month: a microservice is cost-efficient
 * when ARPU is significantly greater than the per-user infrastructure cost.
 * Each architectural decision's quality trade-offs with Affordability influence
 * whether the resulting TCO is expected to be neutral (ORTHOGONAL), reduced
 * (PROMOTES), or increased (INHIBITS) relative to the base egress cost.
 *
 * Usage:
 * <pre>
 *   CostEfficiencyCalculator calc = new CostEfficiencyCalculator(decisions)
 *       .withEgressCost(egressUsd)
 *       .withCloudInfraCost(infraUsd)
 *       .withFinOpsSaving(savingUsd)
 *       .withEffectiveRps(rps)
 *       .withConsumerCount(users)
 *       .withRevenuePerUserPerMonth(revenue);
 *   UnitEconomicsResponse result = calc.calculateUnitEconomics();
 * </pre>
 */
public class CostEfficiencyCalculator {

    // ── Constants (mirrored from UnitEconomicsController) ───────────────────
    private static final double SECONDS_PER_MONTH = 2_592_000.0;
    private static final int    MONTHS_PER_YEAR   = 12;
    private static final int    DAYS_PER_MONTH    = 30;

    // ── Architectural context ────────────────────────────────────────────────
    /**
     * The architectural decisions being analysed. Each decision carries one or
     * more QualityTradeoff entries; the ones whose ArchitecturalCharacteristic
     * is AFFORDABILITY determine whether this calculator applies a cost
     * multiplier (INHIBITS → cost increases), leaves costs unchanged
     * (ORTHOGONAL), or expects savings (PROMOTES → cost decreases).
     */
    private final List<ArchitecturalDecision> architecturalDecisions;

    // ── Cost inputs ──────────────────────────────────────────────────────────
    private double egressTransferCostUsd   = 0.0;
    private double cloudInfraCostUsd       = 0.0;
    private double finopsSavingUsd         = 0.0;
    private int    effectiveRps            = 0;
    private int    consumerCount           = 1;
    private double revenuePerUserPerMonth  = 0.0;

    // ── Constructor ──────────────────────────────────────────────────────────
    public CostEfficiencyCalculator(List<ArchitecturalDecision> architecturalDecisions) {
        if (architecturalDecisions == null) {
            throw new IllegalArgumentException("architecturalDecisions must not be null");
        }
        this.architecturalDecisions = architecturalDecisions;
    }

    // ── Fluent setters ───────────────────────────────────────────────────────
    public CostEfficiencyCalculator withEgressCost(double egressTransferCostUsd) {
        this.egressTransferCostUsd = egressTransferCostUsd;
        return this;
    }

    public CostEfficiencyCalculator withCloudInfraCost(double cloudInfraCostUsd) {
        this.cloudInfraCostUsd = cloudInfraCostUsd;
        return this;
    }

    public CostEfficiencyCalculator withFinOpsSaving(double finopsSavingUsd) {
        this.finopsSavingUsd = Math.max(0, finopsSavingUsd);
        return this;
    }

    public CostEfficiencyCalculator withEffectiveRps(int effectiveRps) {
        this.effectiveRps = effectiveRps;
        return this;
    }

    public CostEfficiencyCalculator withConsumerCount(int consumerCount) {
        this.consumerCount = Math.max(1, consumerCount);
        return this;
    }

    public CostEfficiencyCalculator withRevenuePerUserPerMonth(double revenuePerUserPerMonth) {
        this.revenuePerUserPerMonth = revenuePerUserPerMonth;
        return this;
    }

    // ── Core calculation ─────────────────────────────────────────────────────

    /**
     * Computes the full unit economics response for the configured service.
     *
     * The affordability trade-off summary derived from the architectural
     * decisions is available via {@link #summariseAffordabilityImpact()} —
     * callers can log or expose this alongside the response to explain whether
     * the selected tactics are expected to increase, decrease, or leave
     * unchanged the TCO.
     *
     * @return a fully populated UnitEconomicsResponse
     */
    public UnitEconomicsResponse calculateUnitEconomics() {
        UnitEconomicsResponse resp = new UnitEconomicsResponse();

        int    safeConsumers = Math.max(1, consumerCount);
        double totalTco      = Math.max(0, egressTransferCostUsd + cloudInfraCostUsd);
        long   monthlyReqs   = (long) effectiveRps * (long) SECONDS_PER_MONTH;

        // ── TCO ──────────────────────────────────────────────────────────────
        resp.totalMonthlyTcoUsd   = round2(totalTco);
        resp.totalAnnualTcoUsd    = round2(totalTco * MONTHS_PER_YEAR);
        resp.egressCostUsd        = round2(egressTransferCostUsd);
        resp.cloudInfraCostUsd    = round2(cloudInfraCostUsd);
        resp.finopsSavingUsd      = round2(finopsSavingUsd);
        resp.totalMonthlyRequests = monthlyReqs;

        // ── Unit costs ───────────────────────────────────────────────────────
        resp.costPerRequestUsd      = monthlyReqs > 0 ? round6(totalTco / monthlyReqs) : 0;
        resp.costPerUserPerMonthUsd = round4(totalTco / safeConsumers);
        resp.costPerUserPerDayUsd   = round6(resp.costPerUserPerMonthUsd / DAYS_PER_MONTH);

        // ── ROI / Profitability (only when revenue data is provided) ─────────
        double revenue = revenuePerUserPerMonth;
        resp.hasRevenueData = revenue > 0;

        if (resp.hasRevenueData) {
            double totalMonthlyRevenue = revenue * safeConsumers;
            double netProfit           = totalMonthlyRevenue - totalTco;

            resp.totalMonthlyRevenueUsd  = round2(totalMonthlyRevenue);
            resp.totalAnnualRevenueUsd   = round2(totalMonthlyRevenue * MONTHS_PER_YEAR);
            resp.arpuMonthly             = round2(revenue);

            // Key cost-efficiency signal: ARPU vs cost per user
            // A cost-efficient microservice has ARPU >> costPerUserPerMonth
            resp.monthlyRoiPct  = totalTco > 0 ? round2(netProfit / totalTco * 100) : 0;
            resp.annualRoiPct   = resp.monthlyRoiPct; // same ratio, annualised inputs
            resp.netMonthlyProfitUsd   = round2(netProfit);
            resp.netAnnualProfitUsd    = round2(netProfit * MONTHS_PER_YEAR);
            resp.breakEvenUsers        = (revenue > 0 && totalTco > 0)
                    ? (int) Math.ceil(totalTco / revenue) : 0;
            resp.revenuePerDollarInfra = totalTco > 0 ? round2(totalMonthlyRevenue / totalTco) : 0;
            resp.netMarginPerUserMonthly = round4(revenue - resp.costPerUserPerMonthUsd);
        }

        return resp;
    }

    /**
     * Derives a consolidated affordability impact from the architectural
     * decisions held by this calculator.
     *
     * Rules (applied in precedence order):
     *   - If ANY decision INHIBITS Affordability → overall impact is INHIBITS
     *   - Else if ANY decision PROMOTES Affordability → overall impact is PROMOTES
     *   - Otherwise → ORTHOGONAL
     *
     * This is used to explain to the architect whether the chosen tactic set
     * is expected to increase, decrease, or leave unchanged the service TCO.
     *
     * @return the consolidated AffordabilityImpact for the current decision set
     */
    public AffordabilityImpact summariseAffordabilityImpact() {
        boolean anyInhibits = false;
        boolean anyPromotes = false;

        for (ArchitecturalDecision decision : architecturalDecisions) {
            if (decision.getArchitecturalCharacteristic() == null) continue;
            List<?> tradeoffs = decision.getArchitecturalCharacteristic().getQualityTradeoffs();
            if (tradeoffs == null) continue;

            for (Object raw : tradeoffs) {
                // QualityTradeoff carries (ArchitecturalCharacteristic, TradeoffType)
                if (!(raw instanceof com.calculator.domain.model.quality.QualityTradeoff qt)) continue;
                if (qt.getArchitecturalCharacteristic() == null) continue;

                String attrName = qt.getArchitecturalCharacteristic().getName();
                if (!"AFFORDABILITY".equalsIgnoreCase(attrName)) continue;

                TradeoffType type = qt.getTradeoffType();
                if (type == TradeoffType.INHIBITS)  anyInhibits = true;
                if (type == TradeoffType.PROMOTES)  anyPromotes = true;
            }
        }

        if (anyInhibits) return AffordabilityImpact.INHIBITS;
        if (anyPromotes) return AffordabilityImpact.PROMOTES;
        return AffordabilityImpact.ORTHOGONAL;
    }

    /**
     * Returns the architectural decisions held by this calculator.
     */
    public List<ArchitecturalDecision> getArchitecturalDecisions() {
        return architecturalDecisions;
    }

    // ── Consolidated affordability signal ────────────────────────────────────

    /**
     * Consolidated affordability impact across all architectural decisions.
     * Maps directly to the TradeoffType enum values used in the domain model,
     * expressed in cost-facing language for the UnitEconomics context.
     */
    public enum AffordabilityImpact {
        /** At least one tactic increases egress or infra cost (e.g. Retry, TLS, ALB). */
        INHIBITS,
        /** At least one tactic reduces cost without any that increase it (e.g. FinOps RI/SP). */
        PROMOTES,
        /** No tactic has a direct effect on cost (e.g. Timeout, Circuit Breaker). */
        ORTHOGONAL
    }

    // ── Rounding utilities ───────────────────────────────────────────────────
    private static double round2(double v) { return Math.round(v * 100.0)     / 100.0; }
    private static double round4(double v) { return Math.round(v * 10000.0)   / 10000.0; }
    private static double round6(double v) { return Math.round(v * 1000000.0) / 1000000.0; }
}