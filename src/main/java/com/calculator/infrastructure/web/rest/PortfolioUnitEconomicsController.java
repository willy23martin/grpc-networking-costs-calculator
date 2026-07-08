package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.requests.portfolio.PortfolioUnitEconomicsRequest;
import com.calculator.domain.dto.requests.portfolio.ServiceEntry;
import com.calculator.domain.dto.responses.portfolio.PortfolioUnitEconomicsResponse;
import com.calculator.domain.dto.responses.portfolio.ServiceRoiEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.calculator.application.services.utils.MathUtils.*;
import static com.calculator.application.services.calculators.CostEfficiencyCalculator.SECONDS_PER_MONTH;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioUnitEconomicsController {

    @PostMapping("/roi")
    public ResponseEntity<PortfolioUnitEconomicsResponse> calculatePortfolioRoi(
            @RequestBody PortfolioUnitEconomicsRequest portfolioUnitEconomicsRequest) {

        PortfolioUnitEconomicsResponse portfolioUnitEconomicsResponse = new PortfolioUnitEconomicsResponse();
        List<ServiceEntry> serviceEntries = portfolioUnitEconomicsRequest.services != null ? portfolioUnitEconomicsRequest.services : Collections.emptyList();

        portfolioUnitEconomicsResponse.serviceCount = serviceEntries.size();
        portfolioUnitEconomicsResponse.finopsMonthlySaving = portfolioUnitEconomicsRequest.finopsMonthlySaving;
        portfolioUnitEconomicsResponse.ec2BaselineSpend = portfolioUnitEconomicsRequest.ec2BaselineSpend;

        if (serviceEntries.isEmpty()) {
            portfolioUnitEconomicsResponse.note = "No services in portfolio.";
            portfolioUnitEconomicsResponse.hasRevenueData = false;
            return ResponseEntity.ok(portfolioUnitEconomicsResponse);
        }

        /* ── Aggregate totals ── */
        double totalTco = 0;
        double totalRevenue  = 0;
        int totalRps = 0;
        int totalConsumers = 0;
        boolean hasRevenue = false;

        for (ServiceEntry serviceEntry : serviceEntries) {
            totalTco += Math.max(0, serviceEntry.tco);
            totalRps += Math.max(0, serviceEntry.rps);
            totalConsumers += Math.max(0, serviceEntry.consumers);
            double svcRev = serviceEntry.revenuePerUserMonth * Math.max(0, serviceEntry.consumers);
            totalRevenue += svcRev;
            if (serviceEntry.revenuePerUserMonth > 0) hasRevenue = true;
        }

        portfolioUnitEconomicsResponse.totalMonthlyTco = round2(totalTco);
        portfolioUnitEconomicsResponse.totalAnnualTco = round2(totalTco * 12);
        portfolioUnitEconomicsResponse.avgTcoPerService = round2(serviceEntries.isEmpty() ? 0 : totalTco / serviceEntries.size());
        portfolioUnitEconomicsResponse.totalMonthlyRevenue = round2(totalRevenue);
        portfolioUnitEconomicsResponse.totalAnnualRevenue = round2(totalRevenue * 12);
        portfolioUnitEconomicsResponse.totalRps = totalRps;
        portfolioUnitEconomicsResponse.totalConsumers = totalConsumers;
        portfolioUnitEconomicsResponse.hasRevenueData = hasRevenue;

        /* ── ARPU (weighted average: total revenue / total consumers) ── */
        portfolioUnitEconomicsResponse.portfolioArpu = (totalConsumers > 0)
                ? round4(totalRevenue / totalConsumers) : 0;

        /* ── ROI ── */
        double netProfit = totalRevenue - totalTco;
        portfolioUnitEconomicsResponse.netMonthlyProfit = round2(netProfit);
        portfolioUnitEconomicsResponse.netAnnualProfit  = round2(netProfit * 12);

        if (totalTco > 0 && hasRevenue) {
            portfolioUnitEconomicsResponse.monthlyRoi = round2((netProfit / totalTco) * 100);
            portfolioUnitEconomicsResponse.annualRoi  = round2((netProfit * 12 / (totalTco * 12)) * 100);
            portfolioUnitEconomicsResponse.revenuePerDollarInfra = round2(totalRevenue / totalTco);
        } else {
            portfolioUnitEconomicsResponse.monthlyRoi = 0;
            portfolioUnitEconomicsResponse.annualRoi  = 0;
            portfolioUnitEconomicsResponse.revenuePerDollarInfra = 0;
        }

        /* ── Break-even users (portfolio ARPU > 0 needed) ── */
        if (portfolioUnitEconomicsResponse.portfolioArpu > 0 && totalTco > 0) {
            portfolioUnitEconomicsResponse.breakEvenUsers = (int) Math.ceil(totalTco / portfolioUnitEconomicsResponse.portfolioArpu);
        } else {
            portfolioUnitEconomicsResponse.breakEvenUsers = 0;
        }

        /* ── Unit Economics ── */
        double totalReqPerMonth = (double) totalRps * SECONDS_PER_MONTH;
        portfolioUnitEconomicsResponse.costPerRequestUsd  = (totalReqPerMonth > 0)
                ? round6(totalTco / totalReqPerMonth) : 0;
        portfolioUnitEconomicsResponse.costPerUserPerMonth= (totalConsumers > 0)
                ? round4(totalTco / totalConsumers) : 0;
        portfolioUnitEconomicsResponse.costPerUserPerDay  = round6(portfolioUnitEconomicsResponse.costPerUserPerMonth / 30.0);

        /* ── FinOps impact ── */
        double finopsSaving        = Math.max(0, portfolioUnitEconomicsRequest.finopsMonthlySaving);
        portfolioUnitEconomicsResponse.finopsAdjustedTco     = round2(Math.max(0, totalTco - finopsSaving));
        double netProfitFinops     = totalRevenue - portfolioUnitEconomicsResponse.finopsAdjustedTco;
        portfolioUnitEconomicsResponse.finopsAdjustedRoi     = (portfolioUnitEconomicsResponse.finopsAdjustedTco > 0 && hasRevenue)
                ? round2((netProfitFinops / portfolioUnitEconomicsResponse.finopsAdjustedTco) * 100) : 0;
        portfolioUnitEconomicsResponse.finopsRoiImprovementPct = round2(portfolioUnitEconomicsResponse.finopsAdjustedRoi - portfolioUnitEconomicsResponse.monthlyRoi);

        /* ── Per-service breakdown ── */
        for (ServiceEntry serviceEntry : serviceEntries) {
            ServiceRoiEntry serviceRoiEntry = new ServiceRoiEntry();
            serviceRoiEntry.name = serviceEntry.name;
            serviceRoiEntry.buc = serviceEntry.buc;
            serviceRoiEntry.tco = round2(serviceEntry.tco);
            serviceRoiEntry.consumers = serviceEntry.consumers;
            serviceRoiEntry.rps = serviceEntry.rps;
            serviceRoiEntry.arpu = serviceEntry.revenuePerUserMonth;
            serviceRoiEntry.monthlyRevenue = round2(serviceEntry.revenuePerUserMonth * Math.max(0, serviceEntry.consumers));
            serviceRoiEntry.roi = (serviceEntry.tco > 0 && serviceEntry.revenuePerUserMonth > 0)
                    ? round2(((serviceRoiEntry.monthlyRevenue - serviceEntry.tco) / serviceEntry.tco) * 100) : 0;
            serviceRoiEntry.tcoShare = (totalTco > 0)
                    ? round2((serviceEntry.tco / totalTco) * 100) : 0;
            portfolioUnitEconomicsResponse.serviceBreakdown.add(serviceRoiEntry);
        }

        portfolioUnitEconomicsResponse.note = "Aggregate portfolio ROI computed from " + serviceEntries.size()
                + " service" + (serviceEntries.size() != 1 ? "s" : "") + ". "
                + (hasRevenue ? "Revenue data available." : "No revenue data — set Expected Revenue per Transaction in Phase 1.")
                + (finopsSaving > 0 ? " FinOps saving of $" + round2(finopsSaving) + "/mo applied." : "");

        return ResponseEntity.ok(portfolioUnitEconomicsResponse);
    }
}