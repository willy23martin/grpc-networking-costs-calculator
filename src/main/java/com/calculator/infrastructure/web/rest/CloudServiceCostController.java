package com.calculator.infrastructure.web.rest;

import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.Logger;

import static com.calculator.application.services.utils.MathUtils.round2;

@RestController
@RequestMapping("/api/cost")
@CrossOrigin(origins = "*")
public class CloudServiceCostController { // TODO

    private static final Logger log = Logger.getLogger(CloudServiceCostController.class.getName());

    /* ================================================================
       SHARED RESPONSE — used by all endpoints
    ================================================================ */
    public static class ServiceCostResponse {
        @JsonProperty public double       monthlyTotalUsd;
        @JsonProperty public List<String> breakdownLines  = new ArrayList<>();
        @JsonProperty public String       sessionStorageKey;   // e.g. "tco_alb_cost"
        @JsonProperty public String       summaryLabel;        // human-readable label for UI
    }

    /* ================================================================
       POST /api/cost/alb
    ================================================================ */
    public static class AlbCostRequest {
        @JsonProperty public int    albCount         = 1;
        @JsonProperty public double lcuPerHour       = 0.0;
        @JsonProperty public double fixedPerMonthUsd = 16.20;   // from /api/aws/alb-pricing
        @JsonProperty public double lcuPerHourUsd    = 0.008;   // from /api/aws/alb-pricing
    }

    @PostMapping("/alb")
    public ResponseEntity<ServiceCostResponse> calculateAlbCost(
            @RequestBody AlbCostRequest req) {

        log.info("ALBCostRequest: " + req);
        JSONLogger.logAsJSON(log, req);

        ServiceCostResponse resp = new ServiceCostResponse();
        resp.sessionStorageKey = "tco_alb_cost";

        double fixedMonthlyCost = req.fixedPerMonthUsd * req.albCount;
        double lcuMonthlyCost   = req.lcuPerHourUsd * req.lcuPerHour * 730 * req.albCount;
        double totalMonthlyCost = fixedMonthlyCost + lcuMonthlyCost;

        resp.monthlyTotalUsd = round2(totalMonthlyCost);
        resp.summaryLabel    = req.albCount + " ALB" + (req.albCount > 1 ? "s" : "");

        resp.breakdownLines.add("Fixed: $" + round2(fixedMonthlyCost) + "/mo ("
                + req.albCount + " ALB × $" + req.fixedPerMonthUsd + "/mo)");
        if (req.lcuPerHour > 0) {
            resp.breakdownLines.add("LCUs: $" + round2(lcuMonthlyCost) + "/mo ("
                    + req.lcuPerHour + " LCU/hr × $" + req.lcuPerHourUsd + "/hr × 730 hr × "
                    + req.albCount + " ALB)");
        }

        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       POST /api/cost/api-gateway
    ================================================================ */
    public static class ApiGatewayCostRequest {
        @JsonProperty public String apiGatewayType        = "rest"; // rest | http | websocket
        @JsonProperty public double callsPerMonthMillions = 0.0;
        @JsonProperty public double cacheHourlyRate       = 0.0;    // 0 = no cache

        // Prices from /api/aws/api-gateway-pricing
        @JsonProperty public double restApiPer1MCallsMonthly   = 3.50;
        @JsonProperty public double httpApiPer1MCallsFirst1B   = 1.00;
        @JsonProperty public double wsApiPer1MConnections      = 0.25;
    }

    @PostMapping("/api-gateway")
    public ResponseEntity<ServiceCostResponse> calculateApiGatewayCost(
            @RequestBody ApiGatewayCostRequest req) {

        ServiceCostResponse resp = new ServiceCostResponse();
        resp.sessionStorageKey = "tco_apigw_cost";

        double callCost;
        switch (req.apiGatewayType == null ? "rest" : req.apiGatewayType.toLowerCase()) {
            case "http":      callCost = req.callsPerMonthMillions * req.httpApiPer1MCallsFirst1B; break;
            case "websocket": callCost = req.callsPerMonthMillions * req.wsApiPer1MConnections;    break;
            default:          callCost = req.callsPerMonthMillions * req.restApiPer1MCallsMonthly; break;
        }

        double cacheCost = req.cacheHourlyRate * 730;
        double total     = callCost + cacheCost;

        resp.monthlyTotalUsd = round2(total);
        resp.summaryLabel    = "API Gateway (" + req.apiGatewayType.toUpperCase() + ")";

        if (req.callsPerMonthMillions > 0)
            resp.breakdownLines.add("Calls: $" + round2(callCost) + "/mo");
        if (cacheCost > 0)
            resp.breakdownLines.add("Cache: $" + round2(cacheCost) + "/mo");

        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       POST /api/cost/container
    ================================================================ */
    public static class ContainerCostRequest {
        // Cluster assets
        @JsonProperty public boolean eksClusterEnabled         = false;
        @JsonProperty public int     eksClusterCount           = 1;
        @JsonProperty public String  orchestrationType         = "eks"; // eks | ecs

        @JsonProperty public boolean clusterLoadBalancerEnabled = false;
        @JsonProperty public int     clusterLoadBalancerCount   = 1;

        @JsonProperty public boolean hostStorageEnabled         = false;
        @JsonProperty public double  hostStorageGbPerNode       = 30.0;
        @JsonProperty public int     podCount                   = 10;

        @JsonProperty public boolean clusterBackupEnabled       = false;
        @JsonProperty public double  clusterBackupGb            = 50.0;

        @JsonProperty public boolean hostLicenseEnabled         = false;
        @JsonProperty public double  hostOsLicenseRatePerHour   = 0.0;

        @JsonProperty public boolean workloadLicenseEnabled     = false;
        @JsonProperty public double  workloadLicenseMonthlyCost = 0.0;

        @JsonProperty public int     spotDiscountPercent        = 0;

        // Application design
        @JsonProperty public boolean cronJobsOnFargateEnabled   = false;
        @JsonProperty public int     cronJobCount               = 1;
        @JsonProperty public double  cronJobDurationMinutes     = 15.0;
        @JsonProperty public int     cronJobRunsPerDay          = 4;

        @JsonProperty public boolean statefulSetsEnabled        = false;
        @JsonProperty public int     statefulSetReplicaCount    = 1;
        @JsonProperty public double  statefulSetVolumeGb        = 20.0;

        // Prices from /api/aws/container-pricing
        @JsonProperty public double eksControlPlanePerMonth     = 73.0;
        @JsonProperty public double albFixedPerMonth            = 16.43;
        @JsonProperty public double ebsGp3PerGbMonth            = 0.08;
        @JsonProperty public double fargateVcpuPerHour          = 0.04048;
        @JsonProperty public double fargateGbPerHour            = 0.004445;
        @JsonProperty public double fargateSpotVcpuPerHour      = 0.01254688;
        @JsonProperty public double fargateSpotGbPerHour        = 0.00137248;
    }

    @PostMapping("/container")
    public ResponseEntity<ServiceCostResponse> calculateContainerCost(
            @RequestBody ContainerCostRequest req) {

        ServiceCostResponse resp = new ServiceCostResponse();
        resp.sessionStorageKey = "tco_container_cost";

        double S3_STANDARD_PER_GB = 0.023;
        double clusterAssetTotal  = 0;

        // EKS control plane
        if (req.eksClusterEnabled && "eks".equalsIgnoreCase(req.orchestrationType)) {
            double cost = req.eksClusterCount * req.eksControlPlanePerMonth;
            clusterAssetTotal += cost;
            resp.breakdownLines.add("EKS Control Plane (" + req.eksClusterCount
                    + " \u00d7 $" + req.eksControlPlanePerMonth + "/mo): $" + round2(cost) + "/mo");
        } else if (req.eksClusterEnabled) {
            resp.breakdownLines.add("ECS clusters: no control plane charge");
        }

        // Cluster ALBs
        if (req.clusterLoadBalancerEnabled) {
            double cost = req.clusterLoadBalancerCount * req.albFixedPerMonth;
            clusterAssetTotal += cost;
            resp.breakdownLines.add("Cluster ALBs (" + req.clusterLoadBalancerCount
                    + " \u00d7 $" + req.albFixedPerMonth + "/mo): $" + round2(cost) + "/mo");
        }

        // Host OS storage (EBS root volumes)
        if (req.hostStorageEnabled) {
            int nodeCount = Math.max(1, (int) Math.ceil(req.podCount / 10.0));
            double cost   = req.hostStorageGbPerNode * nodeCount * req.ebsGp3PerGbMonth;
            clusterAssetTotal += cost;
            resp.breakdownLines.add("Host Storage (~" + nodeCount + " nodes \u00d7 "
                    + req.hostStorageGbPerNode + " GB \u00d7 $" + req.ebsGp3PerGbMonth
                    + "): $" + round2(cost) + "/mo");
        }

        // Cluster backup (Velero / AWS Backup to S3)
        if (req.clusterBackupEnabled) {
            double cost = req.clusterBackupGb * S3_STANDARD_PER_GB;
            clusterAssetTotal += cost;
            resp.breakdownLines.add("Cluster Backup (" + req.clusterBackupGb
                    + " GB \u00d7 $" + S3_STANDARD_PER_GB + "): $" + round2(cost) + "/mo");
        }

        // Host OS license (Amazon Linux = free; Windows/RHEL = extra)
        if (req.hostLicenseEnabled && req.hostOsLicenseRatePerHour > 0) {
            int nodeCount = Math.max(1, (int) Math.ceil(req.podCount / 10.0));
            double cost   = req.hostOsLicenseRatePerHour * 730 * nodeCount;
            clusterAssetTotal += cost;
            resp.breakdownLines.add("Host OS License: $" + round2(cost) + "/mo");
        }

        // Commercial workload licenses
        if (req.workloadLicenseEnabled && req.workloadLicenseMonthlyCost > 0) {
            clusterAssetTotal += req.workloadLicenseMonthlyCost;
            resp.breakdownLines.add("Workload License: $"
                    + round2(req.workloadLicenseMonthlyCost) + "/mo");
        }

        // Spot discount
        if (req.spotDiscountPercent > 0 && clusterAssetTotal > 0) {
            double saving = clusterAssetTotal * req.spotDiscountPercent / 100.0;
            clusterAssetTotal -= saving;
            resp.breakdownLines.add("Spot discount (" + req.spotDiscountPercent
                    + "%): -$" + round2(saving) + "/mo");
        }

        // Application design: Fargate CronJobs
        double appDesignTotal = 0;
        if (req.cronJobsOnFargateEnabled) {
            double vcpuRate  = req.fargateVcpuPerHour;
            double gbRate    = req.fargateGbPerHour;
            double costPerRun = req.cronJobDurationMinutes * 60
                    * (0.25 * vcpuRate / 3600.0 + 0.5 * gbRate / 3600.0);
            double monthlyCost = costPerRun * req.cronJobRunsPerDay * 30 * req.cronJobCount;
            appDesignTotal += monthlyCost;
            resp.breakdownLines.add("CronJobs on Fargate: $" + round2(monthlyCost) + "/mo");
        }

        // Application design: StatefulSet PVC storage
        if (req.statefulSetsEnabled) {
            double cost = req.statefulSetReplicaCount * req.statefulSetVolumeGb * req.ebsGp3PerGbMonth;
            appDesignTotal += cost;
            resp.breakdownLines.add("StatefulSet PVCs (" + req.statefulSetReplicaCount
                    + " \u00d7 " + req.statefulSetVolumeGb + " GB \u00d7 $"
                    + req.ebsGp3PerGbMonth + "): $" + round2(cost) + "/mo");
        }

        resp.monthlyTotalUsd = round2(clusterAssetTotal + appDesignTotal);
        resp.summaryLabel    = "Containerized environment";
        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       POST /api/cost/finops-discount
    ================================================================ */
    public static class FinOpsDiscountRequest {
        @JsonProperty public double  onDemandMonthlySpend            = 0.0;

        @JsonProperty public boolean standardRi1yrEnabled            = false;
        @JsonProperty public boolean standardRi3yrEnabled            = false;
        @JsonProperty public boolean convertibleRi1yrEnabled         = false;
        @JsonProperty public boolean savingsPlan1yrEnabled           = false;
        @JsonProperty public boolean savingsPlan3yrEnabled           = false;
        @JsonProperty public boolean trustedAdvisorEnabled           = false;

        // Percentages from /api/aws/cost-optimisation
        @JsonProperty public double  reservedInstance1yrSavingsPct   = 40.0;
        @JsonProperty public double  reservedInstance3yrSavingsPct   = 60.0;
        @JsonProperty public double  convertibleRi1yrSavingsPct      = 31.0;
        @JsonProperty public double  savingsPlan1yrSavingsPct        = 40.0;
        @JsonProperty public double  savingsPlan3yrSavingsPct        = 60.0;
        @JsonProperty public double  businessSupportMinMonthUsd      = 100.0;
        @JsonProperty public double  businessSupportPctMonthlyUsage  = 10.0;
    }

    public static class FinOpsDiscountResponse {
        @JsonProperty public double       bestMonthlySavingUsd;
        @JsonProperty public double       adjustedMonthlySpend;
        @JsonProperty public List<String> breakdownLines  = new ArrayList<>();
        @JsonProperty public List<Map<String, Object>> discountRows = new ArrayList<>();
    }

    @PostMapping("/finops-discount")
    public ResponseEntity<FinOpsDiscountResponse> calculateFinOpsDiscount(
            @RequestBody FinOpsDiscountRequest req) {

        FinOpsDiscountResponse resp = new FinOpsDiscountResponse();
        double spend      = req.onDemandMonthlySpend;
        double bestSaving = 0;

        record Option(boolean enabled, double pct, String label) {}
        List<Option> options = List.of(
                new Option(req.standardRi1yrEnabled,      req.reservedInstance1yrSavingsPct, "Standard RI (1-yr)"),
                new Option(req.standardRi3yrEnabled,      req.reservedInstance3yrSavingsPct, "Standard RI (3-yr)"),
                new Option(req.convertibleRi1yrEnabled,   req.convertibleRi1yrSavingsPct,    "Convertible RI (1-yr)"),
                new Option(req.savingsPlan1yrEnabled,      req.savingsPlan1yrSavingsPct,      "Savings Plan (1-yr)"),
                new Option(req.savingsPlan3yrEnabled,      req.savingsPlan3yrSavingsPct,      "Savings Plan (3-yr)")
        );

        for (Option opt : options) {
            if (!opt.enabled()) continue;
            double saving = spend * opt.pct() / 100.0;
            if (saving > bestSaving) bestSaving = saving;
            resp.breakdownLines.add(opt.label() + ": save $" + round2(saving)
                    + "/mo (~" + opt.pct() + "%)");

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("label",           opt.label());
            row.put("savingsPct",      opt.pct());
            row.put("onDemandSpend",   round2(spend));
            row.put("adjustedSpend",   round2(spend - saving));
            resp.discountRows.add(row);
        }

        if (req.trustedAdvisorEnabled) {
            double cost = Math.max(req.businessSupportMinMonthUsd,
                    spend * req.businessSupportPctMonthlyUsage / 100.0);
            resp.breakdownLines.add("Trusted Advisor (Business Support): +$" + round2(cost) + "/mo");
        }

        resp.bestMonthlySavingUsd = round2(bestSaving);
        resp.adjustedMonthlySpend = round2(Math.max(0, spend - bestSaving));
        return ResponseEntity.ok(resp);
    }

}