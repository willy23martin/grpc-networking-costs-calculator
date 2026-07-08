package com.calculator.domain.dto.responses.portfolio;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class PortfolioUnitEconomicsResponse {
    /* ── Portfolio TCO ── */
    @JsonProperty
    public double totalMonthlyTco;
    @JsonProperty
    public double totalAnnualTco;
    @JsonProperty
    public double avgTcoPerService;
    @JsonProperty
    public int serviceCount;

    /* ── Revenue & ARPU ── */
    @JsonProperty
    public double totalMonthlyRevenue;
    @JsonProperty
    public double totalAnnualRevenue;
    @JsonProperty
    public double portfolioArpu;          /* weighted avg revenue/user/mo */
    @JsonProperty
    public int totalConsumers;

    /* ── ROI ── */
    @JsonProperty
    public double monthlyRoi;             /* (revenue - tco) / tco × 100 */
    @JsonProperty
    public double annualRoi;
    @JsonProperty
    public double netMonthlyProfit;       /* revenue - tco */
    @JsonProperty
    public double netAnnualProfit;
    @JsonProperty
    public int breakEvenUsers;         /* users needed to cover portfolio TCO */
    @JsonProperty
    public double revenuePerDollarInfra;  /* revenue / tco */

    /* ── Unit Economics ── */
    @JsonProperty
    public double costPerRequestUsd;      /* tco / (total_rps × seconds_per_month) */
    @JsonProperty
    public double costPerUserPerMonth;
    @JsonProperty
    public double costPerUserPerDay;
    @JsonProperty
    public int totalRps;

    /* ── FinOps Impact ── */
    @JsonProperty
    public double finopsMonthlySaving;
    @JsonProperty
    public double finopsRoiImprovementPct;/* how much ROI improves with FinOps */
    @JsonProperty
    public double finopsAdjustedTco; /* tco after FinOps discount */
    @JsonProperty
    public double finopsAdjustedRoi;
    @JsonProperty
    public double ec2BaselineSpend;

    /* ── Per-service breakdown ── */
    @JsonProperty
    public List<ServiceRoiEntry> serviceBreakdown = new ArrayList<>();

    /* ── Flags ── */
    @JsonProperty
    public boolean hasRevenueData; /* false if no service has revenue configured */
    @JsonProperty
    public String  note;
}