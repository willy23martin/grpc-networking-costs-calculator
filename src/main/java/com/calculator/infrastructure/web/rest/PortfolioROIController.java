package com.calculator.infrastructure.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.calculator.application.services.utils.MathUtils.*;

@RestController
@RequestMapping("/api/portfolio")
@CrossOrigin(origins = "*")
public class PortfolioROIController { // TODO

    private static final double SECONDS_PER_MONTH = 2_592_000.0;

    /* ================================================================
       REQUEST DTO
    ================================================================ */
    public static class PortfolioRoiRequest {
        /** Each service modelled by the architect */
        @JsonProperty public List<ServiceEntry> services = new ArrayList<>();
        /** FinOps saving already applied (monthly, from RI/SP discount) */
        @JsonProperty public double finopsMonthlySaving = 0.0;
        /** EC2/EKS baseline on-demand spend (for FinOps discount display) */
        @JsonProperty public double ec2BaselineSpend    = 0.0;
    }

    public static class ServiceEntry {
        @JsonProperty public String name;
        @JsonProperty public String buc;
        @JsonProperty public double tco;                 /* monthly TCO in USD */
        @JsonProperty public double revenuePerUserMonth; /* expected monthly revenue per end user */
        @JsonProperty public int    consumers;           /* end users for this service */
        @JsonProperty public int    rps;                 /* requests per second */
        @JsonProperty public double finopsSaving;        /* FinOps saving allocated to this service (optional) */
    }

    /* ================================================================
       RESPONSE DTO
    ================================================================ */
    public static class PortfolioRoiResponse {
        /* ── Portfolio TCO ── */
        @JsonProperty public double totalMonthlyTco;
        @JsonProperty public double totalAnnualTco;
        @JsonProperty public double avgTcoPerService;
        @JsonProperty public int    serviceCount;

        /* ── Revenue & ARPU ── */
        @JsonProperty public double totalMonthlyRevenue;
        @JsonProperty public double totalAnnualRevenue;
        @JsonProperty public double portfolioArpu;          /* weighted avg revenue/user/mo */
        @JsonProperty public int    totalConsumers;

        /* ── ROI ── */
        @JsonProperty public double monthlyRoi;             /* (revenue - tco) / tco × 100 */
        @JsonProperty public double annualRoi;
        @JsonProperty public double netMonthlyProfit;       /* revenue - tco */
        @JsonProperty public double netAnnualProfit;
        @JsonProperty public int    breakEvenUsers;         /* users needed to cover portfolio TCO */
        @JsonProperty public double revenuePerDollarInfra;  /* revenue / tco */

        /* ── Unit Economics ── */
        @JsonProperty public double costPerRequestUsd;      /* tco / (total_rps × seconds_per_month) */
        @JsonProperty public double costPerUserPerMonth;
        @JsonProperty public double costPerUserPerDay;
        @JsonProperty public int    totalRps;

        /* ── FinOps Impact ── */
        @JsonProperty public double finopsMonthlySaving;
        @JsonProperty public double finopsRoiImprovementPct;/* how much ROI improves with FinOps */
        @JsonProperty public double finopsAdjustedTco;      /* tco after FinOps discount */
        @JsonProperty public double finopsAdjustedRoi;
        @JsonProperty public double ec2BaselineSpend;

        /* ── Per-service breakdown ── */
        @JsonProperty public List<ServiceRoiEntry> serviceBreakdown = new ArrayList<>();

        /* ── Flags ── */
        @JsonProperty public boolean hasRevenueData;        /* false if no service has revenue configured */
        @JsonProperty public String  note;
    }

    public static class ServiceRoiEntry {
        @JsonProperty public String name;
        @JsonProperty public String buc;
        @JsonProperty public double tco;
        @JsonProperty public double monthlyRevenue;
        @JsonProperty public double roi;
        @JsonProperty public double arpu;
        @JsonProperty public int    consumers;
        @JsonProperty public int    rps;
        @JsonProperty public double tcoShare;               /* % of total portfolio TCO */
    }

    /* ================================================================
       ENDPOINT
    ================================================================ */
    @PostMapping("/roi")
    public ResponseEntity<PortfolioRoiResponse> calculatePortfolioRoi(
            @RequestBody PortfolioRoiRequest req) {

        PortfolioRoiResponse resp = new PortfolioRoiResponse();
        List<ServiceEntry> svcs = req.services != null ? req.services : Collections.emptyList();

        resp.serviceCount         = svcs.size();
        resp.finopsMonthlySaving  = req.finopsMonthlySaving;
        resp.ec2BaselineSpend     = req.ec2BaselineSpend;

        if (svcs.isEmpty()) {
            resp.note = "No services in portfolio.";
            resp.hasRevenueData = false;
            return ResponseEntity.ok(resp);
        }

        /* ── Aggregate totals ── */
        double totalTco      = 0;
        double totalRevenue  = 0;
        int    totalRps      = 0;
        int    totalConsumers= 0;
        boolean hasRevenue   = false;

        for (ServiceEntry s : svcs) {
            totalTco       += Math.max(0, s.tco);
            totalRps       += Math.max(0, s.rps);
            totalConsumers += Math.max(0, s.consumers);
            double svcRev  = s.revenuePerUserMonth * Math.max(0, s.consumers);
            totalRevenue   += svcRev;
            if (s.revenuePerUserMonth > 0) hasRevenue = true;
        }

        resp.totalMonthlyTco    = round2(totalTco);
        resp.totalAnnualTco     = round2(totalTco * 12);
        resp.avgTcoPerService   = round2(svcs.isEmpty() ? 0 : totalTco / svcs.size());
        resp.totalMonthlyRevenue= round2(totalRevenue);
        resp.totalAnnualRevenue = round2(totalRevenue * 12);
        resp.totalRps           = totalRps;
        resp.totalConsumers     = totalConsumers;
        resp.hasRevenueData     = hasRevenue;

        /* ── ARPU (weighted average: total revenue / total consumers) ── */
        resp.portfolioArpu = (totalConsumers > 0)
                ? round4(totalRevenue / totalConsumers) : 0;

        /* ── ROI ── */
        double netProfit = totalRevenue - totalTco;
        resp.netMonthlyProfit = round2(netProfit);
        resp.netAnnualProfit  = round2(netProfit * 12);

        if (totalTco > 0 && hasRevenue) {
            resp.monthlyRoi = round2((netProfit / totalTco) * 100);
            resp.annualRoi  = round2((netProfit * 12 / (totalTco * 12)) * 100);
            resp.revenuePerDollarInfra = round2(totalRevenue / totalTco);
        } else {
            resp.monthlyRoi = 0;
            resp.annualRoi  = 0;
            resp.revenuePerDollarInfra = 0;
        }

        /* ── Break-even users (portfolio ARPU > 0 needed) ── */
        if (resp.portfolioArpu > 0 && totalTco > 0) {
            resp.breakEvenUsers = (int) Math.ceil(totalTco / resp.portfolioArpu);
        } else {
            resp.breakEvenUsers = 0;
        }

        /* ── Unit Economics ── */
        double totalReqPerMonth = (double) totalRps * SECONDS_PER_MONTH;
        resp.costPerRequestUsd  = (totalReqPerMonth > 0)
                ? round6(totalTco / totalReqPerMonth) : 0;
        resp.costPerUserPerMonth= (totalConsumers > 0)
                ? round4(totalTco / totalConsumers) : 0;
        resp.costPerUserPerDay  = round6(resp.costPerUserPerMonth / 30.0);

        /* ── FinOps impact ── */
        double finopsSaving        = Math.max(0, req.finopsMonthlySaving);
        resp.finopsAdjustedTco     = round2(Math.max(0, totalTco - finopsSaving));
        double netProfitFinops     = totalRevenue - resp.finopsAdjustedTco;
        resp.finopsAdjustedRoi     = (resp.finopsAdjustedTco > 0 && hasRevenue)
                ? round2((netProfitFinops / resp.finopsAdjustedTco) * 100) : 0;
        resp.finopsRoiImprovementPct = round2(resp.finopsAdjustedRoi - resp.monthlyRoi);

        /* ── Per-service breakdown ── */
        for (ServiceEntry s : svcs) {
            ServiceRoiEntry se = new ServiceRoiEntry();
            se.name         = s.name;
            se.buc          = s.buc;
            se.tco          = round2(s.tco);
            se.consumers    = s.consumers;
            se.rps          = s.rps;
            se.arpu         = s.revenuePerUserMonth;
            se.monthlyRevenue = round2(s.revenuePerUserMonth * Math.max(0, s.consumers));
            se.roi          = (s.tco > 0 && s.revenuePerUserMonth > 0)
                    ? round2(((se.monthlyRevenue - s.tco) / s.tco) * 100) : 0;
            se.tcoShare     = (totalTco > 0)
                    ? round2((s.tco / totalTco) * 100) : 0;
            resp.serviceBreakdown.add(se);
        }

        resp.note = "Aggregate portfolio ROI computed from " + svcs.size()
                + " service" + (svcs.size() != 1 ? "s" : "") + ". "
                + (hasRevenue ? "Revenue data available." : "No revenue data — set Expected Revenue per Transaction in Phase 1.")
                + (finopsSaving > 0 ? " FinOps saving of $" + round2(finopsSaving) + "/mo applied." : "");

        return ResponseEntity.ok(resp);
    }
}