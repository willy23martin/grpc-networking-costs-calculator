package com.calculator.domain.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TCOUnitEconomicsResponse {
    /* ── TCO ── */
    @JsonProperty
    public double totalMonthlyTcoUsd;
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
    @JsonProperty public String affordabilityImpact; // "INHIBITS" | "PROMOTES" | "ORTHOGONAL"
}
