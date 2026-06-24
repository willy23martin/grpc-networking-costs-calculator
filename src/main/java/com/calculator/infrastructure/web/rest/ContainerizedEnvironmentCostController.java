package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.alb.aws.alb.AWSALBCostCalculator;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/aws")
@CrossOrigin(origins = "*")
public class ContainerizedEnvironmentCostController {

    private static final java.util.logging.Logger log = Logger.getLogger(ContainerizedEnvironmentCostController.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();

    /* ================================================================
       HARDCODED FALLBACKS (updated Q1-2025)
       Used when AWS Pricing API is unavailable or returns no data.
    ================================================================ */

    // EKS control plane: flat rate per cluster per hour
    private static final double EKS_CONTROL_PLANE_PER_HOUR     = 0.10;
    private static final double EKS_CONTROL_PLANE_PER_MONTH    = 0.10 * 730;   // $73.00/mo

    // Fargate compute: per vCPU-hour and per GB-hour
    private static final double FARGATE_VCPU_PER_HOUR           = 0.04048;
    private static final double FARGATE_GB_PER_HOUR             = 0.004445;
    private static final double FARGATE_SPOT_VCPU_PER_HOUR      = 0.01254688; // ~69% off
    private static final double FARGATE_SPOT_GB_PER_HOUR        = 0.00137248;

    // EBS gp3 default: $0.08/GB-month; provisioned IOPS/throughput extra
    private static final double EBS_GP3_PER_GB_MONTH            = 0.08;
    private static final double EBS_GP3_IOPS_PER_IOPS_MONTH     = 0.005;  // beyond 3000 free
    private static final double EBS_GP3_THROUGHPUT_PER_MBPS     = 0.040;  // beyond 125 MiB/s free

    // ALB (same as AWSALBCostCalculator)
    private static final double ALB_FIXED_PER_HOUR              = 0.0225;
    private static final double ALB_LCU_PER_HOUR                = 0.008;
    private static final double ALB_FIXED_PER_MONTH             = 0.0225 * 730; // $16.425/mo

    // ECR image storage
    private static final double ECR_STORAGE_PER_GB_MONTH        = 0.10;
    private static final double ECR_DATA_TRANSFER_PER_GB        = 0.09;  // same as S3 egress

    // AWS WAF
    private static final double WAF_WEB_ACL_PER_MONTH           = 5.00;
    private static final double WAF_RULE_PER_MONTH              = 1.00;
    private static final double WAF_PER_1M_REQUESTS             = 0.60;

    // EC2 instance approximate on-demand prices (us-east-1, Linux, on-demand, Q1-2025)
    private static final Map<String, Double> EC2_FALLBACK_PRICES = new HashMap<>();
    static {
        EC2_FALLBACK_PRICES.put("t3.medium",    0.0416);
        EC2_FALLBACK_PRICES.put("t3.large",     0.0832);
        EC2_FALLBACK_PRICES.put("m6i.large",    0.096);
        EC2_FALLBACK_PRICES.put("m6i.xlarge",   0.192);
        EC2_FALLBACK_PRICES.put("m6i.2xlarge",  0.384);
        EC2_FALLBACK_PRICES.put("c6i.large",    0.085);
        EC2_FALLBACK_PRICES.put("c6i.xlarge",   0.170);
        EC2_FALLBACK_PRICES.put("m6g.large",    0.077);   // Graviton
        EC2_FALLBACK_PRICES.put("m6g.xlarge",   0.154);   // Graviton
        EC2_FALLBACK_PRICES.put("m6g.2xlarge",  0.308);   // Graviton
        EC2_FALLBACK_PRICES.put("c6g.large",    0.068);   // Graviton
        EC2_FALLBACK_PRICES.put("c6g.xlarge",   0.136);   // Graviton
        EC2_FALLBACK_PRICES.put("r6g.large",    0.1008);  // Graviton
    }

    // API Gateway pricing (us-east-1, Q1-2025)
    private static final double APIGW_REST_PER_1M_CALLS          = 3.50;
    private static final double APIGW_HTTP_PER_1M_CALLS_FIRST1B  = 1.00;
    private static final double APIGW_HTTP_PER_1M_CALLS_OVER1B   = 0.90;
    private static final double APIGW_WS_PER_1M_CONNECTIONS      = 0.25;
    private static final double APIGW_WS_PER_1M_MESSAGES         = 1.00;
    private static final double APIGW_CACHE_05GB_PER_HOUR        = 0.020;
    private static final double APIGW_CACHE_1GB_PER_HOUR         = 0.038;
    private static final double APIGW_CACHE_1_6GB_PER_HOUR      = 0.054;
    private static final double APIGW_CACHE_6_1GB_PER_HOUR      = 0.200;

    private static final String NOTE_SOURCE     = "AWS Pricing API (us-east-1) — live Q1-2025";

    /* ================================================================
       RESPONSE DTOs (unchanged)
    ================================================================ */
    public static class ContainerPricingResponse {
        @JsonProperty public double eksControlPlanePerMonth       = EKS_CONTROL_PLANE_PER_MONTH;
        @JsonProperty public double eksControlPlanePerHour        = EKS_CONTROL_PLANE_PER_HOUR;
        @JsonProperty public double fargateVcpuPerHour            = FARGATE_VCPU_PER_HOUR;
        @JsonProperty public double fargateGbPerHour              = FARGATE_GB_PER_HOUR;
        @JsonProperty public double fargateSpotVcpuPerHour        = FARGATE_SPOT_VCPU_PER_HOUR;
        @JsonProperty public double fargateSpotGbPerHour          = FARGATE_SPOT_GB_PER_HOUR;
        @JsonProperty public double ebsGp3PerGbMonth              = EBS_GP3_PER_GB_MONTH;
        @JsonProperty public double ebsGp3IopsPerIopsMonth        = EBS_GP3_IOPS_PER_IOPS_MONTH;
        @JsonProperty public double ebsGp3ThroughputPerMbpsMonth  = EBS_GP3_THROUGHPUT_PER_MBPS;  // ✅ ADDED
        @JsonProperty public double albFixedPerMonth              = ALB_FIXED_PER_MONTH;
        @JsonProperty public double albFixedPerHour               = ALB_FIXED_PER_HOUR;
        @JsonProperty public double albLcuPerHour                 = ALB_LCU_PER_HOUR;
        @JsonProperty public double ecrStoragePerGbMonth          = ECR_STORAGE_PER_GB_MONTH;
        @JsonProperty public double ecrDataTransferPerGb          = ECR_DATA_TRANSFER_PER_GB;
        @JsonProperty public double wafWebAclPerMonth             = WAF_WEB_ACL_PER_MONTH;
        @JsonProperty public double wafRulePerMonth               = WAF_RULE_PER_MONTH;
        @JsonProperty public double wafPer1MRequests              = WAF_PER_1M_REQUESTS;
        @JsonProperty public Map<String, Double> ec2OnDemandPrices= new LinkedHashMap<>();
        @JsonProperty public String source;
        @JsonProperty public String note;
    }

    public static class ApiGatewayPricingResponse {
        @JsonProperty public double restApiPer1MCallsMonthly      = APIGW_REST_PER_1M_CALLS;
        @JsonProperty public double httpApiPer1MCallsFirst1B      = APIGW_HTTP_PER_1M_CALLS_FIRST1B;
        @JsonProperty public double httpApiPer1MCallsOver1B       = APIGW_HTTP_PER_1M_CALLS_OVER1B;
        @JsonProperty public double wsApiPer1MConnections         = APIGW_WS_PER_1M_CONNECTIONS;
        @JsonProperty public double wsApiPer1MMessages            = APIGW_WS_PER_1M_MESSAGES;
        @JsonProperty public double cacheHalfGbPerHour            = APIGW_CACHE_05GB_PER_HOUR;
        @JsonProperty public double cache1GbPerHour               = APIGW_CACHE_1GB_PER_HOUR;
        @JsonProperty public double cache1_6GbPerHour             = APIGW_CACHE_1_6GB_PER_HOUR;
        @JsonProperty public double cache6_1GbPerHour             = APIGW_CACHE_6_1GB_PER_HOUR;
        @JsonProperty public String source;
        @JsonProperty public String note;
    }

    public static class ContainerTcoRequest {
        // [unchanged - all DTOs remain the same]
        @JsonProperty public int     clusterCount              = 1;
        @JsonProperty public String  orchestrationType         = "eks";
        @JsonProperty public String  underlyingResources       = "ec2-x86";
        @JsonProperty public String  ec2InstanceType           = "m6i.large";
        @JsonProperty public int     nodeCount                 = 0;
        @JsonProperty public int     podCount                  = 20;
        @JsonProperty public int     podsPerNode               = 10;
        @JsonProperty public double  fargateVcpuPerPod         = 0.25;
        @JsonProperty public double  fargateGbPerPod           = 0.5;
        @JsonProperty public int     fargateActivePods         = 20;
        @JsonProperty public int     riDiscountPct             = 0;
        @JsonProperty public int     spotDiscountPct           = 0;
        @JsonProperty public int     subscriptionDiscountPct   = 0;
        @JsonProperty public double  hostStorageGbPerNode      = 30;
        @JsonProperty public double  clusterBackupGb           = 0;
        @JsonProperty public double  pvcGbPerReplica           = 0;
        @JsonProperty public int     statefulSetReplicas       = 0;
        @JsonProperty public int     clusterLbCount            = 0;
        @JsonProperty public double  clusterLbLcuPerHour       = 5;
        @JsonProperty public double  hostOsLicenseRatePerHour  = 0;
        @JsonProperty public double  workloadLicenseCostPerMonth = 0;
        @JsonProperty public boolean clusterWaf                = false;
        @JsonProperty public int     wafRuleCount              = 5;
        @JsonProperty public int     cronJobCount              = 0;
        @JsonProperty public double  cronJobDurationMinutes    = 15;
        @JsonProperty public double  cronJobRunsPerDay         = 4;
        @JsonProperty public double  ecrImageStorageGb         = 0;
    }

    public static class ContainerTcoResponse {
        @JsonProperty public double eksControlPlaneCost       = 0;
        @JsonProperty public double ec2NodesCost              = 0;
        @JsonProperty public double fargateCost               = 0;
        @JsonProperty public double hostStorageCost           = 0;
        @JsonProperty public double backupStorageCost         = 0;
        @JsonProperty public double pvcStorageCost            = 0;
        @JsonProperty public double clusterLbCost             = 0;
        @JsonProperty public double wafCost                   = 0;
        @JsonProperty public double ecrCost                   = 0;
        @JsonProperty public double hostOsLicenseCost         = 0;
        @JsonProperty public double workloadLicenseCost       = 0;
        @JsonProperty public double cronJobFargateCost        = 0;
        @JsonProperty public double discountSaving            = 0;
        @JsonProperty public double totalMonthlyContainerTco  = 0;
        @JsonProperty public Map<String, String> lineItems    = new LinkedHashMap<>();
        @JsonProperty public String ec2InstanceType;
        @JsonProperty public double ec2InstancePricePerHour;
        @JsonProperty public int    estimatedNodeCount;
        @JsonProperty public String source;
        @JsonProperty public String note;
    }

    /* ================================================================
       ENDPOINT 1: GET /api/aws/container-pricing
    ================================================================ */
    @GetMapping("/container-pricing")
    public ResponseEntity<ContainerPricingResponse> getContainerPricing() {
        ContainerPricingResponse resp = new ContainerPricingResponse();

        // ✅ Explicitly initialize ALL @JsonProperty fields with fallback values
        resp.eksControlPlanePerMonth       = EKS_CONTROL_PLANE_PER_MONTH;
        resp.eksControlPlanePerHour        = EKS_CONTROL_PLANE_PER_HOUR;
        resp.fargateVcpuPerHour            = FARGATE_VCPU_PER_HOUR;
        resp.fargateGbPerHour              = FARGATE_GB_PER_HOUR;
        resp.fargateSpotVcpuPerHour        = FARGATE_SPOT_VCPU_PER_HOUR;
        resp.fargateSpotGbPerHour          = FARGATE_SPOT_GB_PER_HOUR;
        resp.ebsGp3PerGbMonth              = EBS_GP3_PER_GB_MONTH;
        resp.ebsGp3IopsPerIopsMonth        = EBS_GP3_IOPS_PER_IOPS_MONTH;
        resp.ebsGp3ThroughputPerMbpsMonth  = EBS_GP3_THROUGHPUT_PER_MBPS;
        resp.albFixedPerMonth              = ALB_FIXED_PER_MONTH;
        resp.albFixedPerHour               = ALB_FIXED_PER_HOUR;
        resp.albLcuPerHour                 = ALB_LCU_PER_HOUR;
        resp.ecrStoragePerGbMonth          = ECR_STORAGE_PER_GB_MONTH;
        resp.ecrDataTransferPerGb          = ECR_DATA_TRANSFER_PER_GB;
        resp.wafWebAclPerMonth             = WAF_WEB_ACL_PER_MONTH;
        resp.wafRulePerMonth               = WAF_RULE_PER_MONTH;
        resp.wafPer1MRequests              = WAF_PER_1M_REQUESTS;

        resp.source = NOTE_SOURCE;
        resp.note   = "EKS control plane, Fargate (incl. Spot), EBS gp3 (storage/IOPS/throughput), ALB, ECR, WAF pricing for us-east-1. "
                + "EC2 on-demand prices for common node group instance types. "
                + "Spot and RI discounts applied client-side or via /container-tco.";

        // Populate EC2 prices: try live API, fall back to hardcoded
        resp.ec2OnDemandPrices.putAll(EC2_FALLBACK_PRICES); // start with fallbacks
        try {
            enrichEc2Prices(resp.ec2OnDemandPrices);
            resp.source = "AWS Pricing API (live) + fallback for unavailable types";
        } catch (Exception e) {
            log.warning("Could not fetch EC2 prices from Pricing API:" + e.getMessage());
            resp.source = "Hardcoded fallback (Q1-2025) — AWS Pricing API unavailable";
        }

        // Try to get live Fargate prices (override fallbacks if successful)
        try {
            double[] fargatePrices = fetchFargatePrices();
            resp.fargateVcpuPerHour = fargatePrices[0];
            resp.fargateGbPerHour   = fargatePrices[1];
        } catch (Exception e) {
            log.warning("Could not fetch Fargate prices: " + e.getMessage());
        }

        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       ENDPOINT 2: GET /api/aws/api-gateway-pricing
    ================================================================ */
    @GetMapping("/api-gateway-pricing")
    public ResponseEntity<ApiGatewayPricingResponse> getApiGatewayPricing() {
        ApiGatewayPricingResponse resp = new ApiGatewayPricingResponse();

        // Initialize ALL fields with fallback values
        resp.restApiPer1MCallsMonthly      = APIGW_REST_PER_1M_CALLS;
        resp.httpApiPer1MCallsFirst1B      = APIGW_HTTP_PER_1M_CALLS_FIRST1B;
        resp.httpApiPer1MCallsOver1B       = APIGW_HTTP_PER_1M_CALLS_OVER1B;
        resp.wsApiPer1MConnections         = APIGW_WS_PER_1M_CONNECTIONS;
        resp.wsApiPer1MMessages            = APIGW_WS_PER_1M_MESSAGES;
        resp.cacheHalfGbPerHour            = APIGW_CACHE_05GB_PER_HOUR;
        resp.cache1GbPerHour               = APIGW_CACHE_1GB_PER_HOUR;
        resp.cache1_6GbPerHour             = APIGW_CACHE_1_6GB_PER_HOUR;
        resp.cache6_1GbPerHour             = APIGW_CACHE_6_1GB_PER_HOUR;

        resp.source = NOTE_SOURCE;
        resp.note   = "API Gateway pricing for REST, HTTP, and WebSocket APIs in us-east-1. "
                + "REST API: $3.50/million calls. HTTP API: $1.00/million (first 1B), $0.90 thereafter. "
                + "WebSocket: $0.25/million connection-minutes + $1.00/million messages. "
                + "Optional caching from $0.020/hr (0.5 GB) to $0.200/hr (6.1 GB).";

        // Try to get live API Gateway prices (only overrides available fields)
        try {
            Map<String, Double> livePrices = fetchApiGatewayPrices();
            if (livePrices.containsKey("rest-per-1m"))
                resp.restApiPer1MCallsMonthly  = livePrices.get("rest-per-1m");
            if (livePrices.containsKey("http-per-1m-first"))
                resp.httpApiPer1MCallsFirst1B  = livePrices.get("http-per-1m-first");
            resp.source = "AWS Pricing API (live)";
        } catch (Exception e) {
            log.warning("Could not fetch API Gateway prices: " + e.getMessage());
            resp.source = "Hardcoded fallback (Q1-2025) — AWS Pricing API unavailable";
        }

        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       ENDPOINT 3: POST /api/aws/container-tco [unchanged business logic]
    ================================================================ */

    @PostMapping("/container-tco")
    public ResponseEntity<ContainerTcoResponse> calculateContainerTco(@RequestBody ContainerTcoRequest req) {
        ContainerTcoResponse resp = new ContainerTcoResponse();
        resp.ec2InstanceType = req.ec2InstanceType;
        resp.source = NOTE_SOURCE;

        double total = 0.0;

        // ── 1. EKS Control Plane ──────────────────────────────────
        if ("eks".equalsIgnoreCase(req.orchestrationType)) {
            resp.eksControlPlaneCost = EKS_CONTROL_PLANE_PER_MONTH * req.clusterCount;
            resp.lineItems.put("EKS Control Plane",
                    String.format("%d cluster(s) × $%.2f/mo = $%.2f/mo",
                            req.clusterCount, EKS_CONTROL_PLANE_PER_MONTH, resp.eksControlPlaneCost));
            total += resp.eksControlPlaneCost;
        }

        // ── 2. EC2 Nodes OR Fargate compute ──────────────────────
        int estimatedNodes = req.nodeCount > 0
                ? req.nodeCount
                : (int) Math.max(1, Math.ceil((double) req.podCount / req.podsPerNode));
        resp.estimatedNodeCount = estimatedNodes;

        boolean isFargate = req.underlyingResources != null
                && req.underlyingResources.toLowerCase().contains("fargate");

        if (isFargate) {
            boolean isSpot = req.underlyingResources.toLowerCase().contains("spot");
            double vcpuRate = isSpot ? FARGATE_SPOT_VCPU_PER_HOUR : FARGATE_VCPU_PER_HOUR;
            double gbRate   = isSpot ? FARGATE_SPOT_GB_PER_HOUR   : FARGATE_GB_PER_HOUR;
            int    pods     = req.fargateActivePods > 0 ? req.fargateActivePods : req.podCount;
            resp.fargateCost = (req.fargateVcpuPerPod * vcpuRate + req.fargateGbPerPod * gbRate)
                    * pods * 730;
            resp.lineItems.put("Fargate Compute",
                    String.format("%d pods × (%.2f vCPU × $%.5f/hr + %.2f GB × $%.5f/hr) × 730 hr = $%.2f/mo",
                            pods, req.fargateVcpuPerPod, vcpuRate, req.fargateGbPerPod, gbRate, resp.fargateCost));
            total += resp.fargateCost;
        } else {
            // EC2 node group
            double ec2PricePerHour = fetchEc2Price(req.ec2InstanceType);
            resp.ec2InstancePricePerHour = ec2PricePerHour;
            resp.ec2NodesCost = ec2PricePerHour * 730 * estimatedNodes;
            resp.lineItems.put("EC2 Node Group",
                    String.format("%d nodes × $%.4f/hr × 730 hr = $%.2f/mo",
                            estimatedNodes, ec2PricePerHour, resp.ec2NodesCost));
            total += resp.ec2NodesCost;
        }

        // ── 3. Host OS Storage (EBS gp3 root volume per node) ────
        if (req.hostStorageGbPerNode > 0 && !isFargate) {
            resp.hostStorageCost = req.hostStorageGbPerNode * estimatedNodes * EBS_GP3_PER_GB_MONTH;
            resp.lineItems.put("Host OS Storage (EBS gp3)",
                    String.format("%d nodes × %.0f GB × $%.2f/GB-mo = $%.2f/mo",
                            estimatedNodes, req.hostStorageGbPerNode, EBS_GP3_PER_GB_MONTH, resp.hostStorageCost));
            total += resp.hostStorageCost;
        }

        // ── 4. Cluster Backup Storage (S3) ────────────────────────
        if (req.clusterBackupGb > 0) {
            resp.backupStorageCost = req.clusterBackupGb * 0.023; // S3 Standard
            resp.lineItems.put("Cluster Backup (S3)",
                    String.format("%.0f GB × $0.023/GB-mo = $%.2f/mo",
                            req.clusterBackupGb, resp.backupStorageCost));
            total += resp.backupStorageCost;
        }

        // ── 5. StatefulSet PVC Storage (EBS gp3) ─────────────────
        if (req.statefulSetReplicas > 0 && req.pvcGbPerReplica > 0) {
            resp.pvcStorageCost = req.statefulSetReplicas * req.pvcGbPerReplica * EBS_GP3_PER_GB_MONTH;
            resp.lineItems.put("StatefulSet PVC Storage (EBS gp3)",
                    String.format("%d replicas × %.0f GB × $%.2f/GB-mo = $%.2f/mo",
                            req.statefulSetReplicas, req.pvcGbPerReplica, EBS_GP3_PER_GB_MONTH, resp.pvcStorageCost));
            total += resp.pvcStorageCost;
        }

        // ── 6. Cluster Load Balancers (ALB) ───────────────────────
        if (req.clusterLbCount > 0) {
            resp.clusterLbCost = (ALB_FIXED_PER_HOUR + ALB_LCU_PER_HOUR * req.clusterLbLcuPerHour)
                    * 730 * req.clusterLbCount;
            resp.lineItems.put("Cluster ALBs",
                    String.format("%d LBs × ($%.4f/hr + %.1f LCU × $%.4f/hr) × 730 hr = $%.2f/mo",
                            req.clusterLbCount, ALB_FIXED_PER_HOUR, req.clusterLbLcuPerHour,
                            ALB_LCU_PER_HOUR, resp.clusterLbCost));
            total += resp.clusterLbCost;
        }

        // ── 7. AWS WAF ────────────────────────────────────────────
        if (req.clusterWaf) {
            // Approximate WAF request volume from pod count × base RPS assumption
            double approxMonthlyReqMillions = req.podCount * 100.0 * 2592000 / 1_000_000;
            resp.wafCost = WAF_WEB_ACL_PER_MONTH
                    + (WAF_RULE_PER_MONTH * req.wafRuleCount)
                    + (WAF_PER_1M_REQUESTS * approxMonthlyReqMillions);
            resp.lineItems.put("AWS WAF",
                    String.format("WebACL $%.2f + %d rules $%.2f + req $%.2f = $%.2f/mo",
                            WAF_WEB_ACL_PER_MONTH, req.wafRuleCount,
                            WAF_RULE_PER_MONTH * req.wafRuleCount,
                            WAF_PER_1M_REQUESTS * approxMonthlyReqMillions, resp.wafCost));
            total += resp.wafCost;
        }

        // ── 8. ECR Image Storage ──────────────────────────────────
        if (req.ecrImageStorageGb > 0) {
            resp.ecrCost = req.ecrImageStorageGb * ECR_STORAGE_PER_GB_MONTH;
            resp.lineItems.put("ECR Image Storage",
                    String.format("%.1f GB × $%.2f/GB-mo = $%.2f/mo",
                            req.ecrImageStorageGb, ECR_STORAGE_PER_GB_MONTH, resp.ecrCost));
            total += resp.ecrCost;
        }

        // ── 9. Host OS License ────────────────────────────────────
        if (req.hostOsLicenseRatePerHour > 0 && !isFargate) {
            resp.hostOsLicenseCost = req.hostOsLicenseRatePerHour * 730 * estimatedNodes;
            resp.lineItems.put("Host OS License",
                    String.format("%d nodes × $%.4f/hr × 730 hr = $%.2f/mo",
                            estimatedNodes, req.hostOsLicenseRatePerHour, resp.hostOsLicenseCost));
            total += resp.hostOsLicenseCost;
        }

        // ── 10. Commercial Workload License ──────────────────────
        if (req.workloadLicenseCostPerMonth > 0) {
            resp.workloadLicenseCost = req.workloadLicenseCostPerMonth;
            resp.lineItems.put("Commercial Workload License",
                    String.format("$%.2f/mo (user-provided)", resp.workloadLicenseCost));
            total += resp.workloadLicenseCost;
        }

        // ── 11. CronJob Fargate Compute ───────────────────────────
        if (req.cronJobCount > 0 && isFargate) {
            boolean isSpot = req.underlyingResources.toLowerCase().contains("spot");
            double vcpuRate = isSpot ? FARGATE_SPOT_VCPU_PER_HOUR : FARGATE_VCPU_PER_HOUR;
            double gbRate = isSpot ? FARGATE_SPOT_GB_PER_HOUR : FARGATE_GB_PER_HOUR;
            double cronSeconds = req.cronJobDurationMinutes * 60 * req.cronJobRunsPerDay * 30 * req.cronJobCount;
            resp.cronJobFargateCost = cronSeconds
                    * (0.25 * vcpuRate / 3600 + 0.5 * gbRate / 3600);
            resp.lineItems.put("CronJob Fargate Compute",
                    String.format("%d jobs × %.0f min × %.0f runs/day × 30 days = $%.2f/mo",
                            req.cronJobCount, req.cronJobDurationMinutes,
                            req.cronJobRunsPerDay, resp.cronJobFargateCost));
            total += resp.cronJobFargateCost;
        }

        // ── 12. Apply Discounts ────────────────────────────────────
        double maxDiscountPct = Math.max(req.riDiscountPct,
                Math.max(req.spotDiscountPct, req.subscriptionDiscountPct));
        if (maxDiscountPct > 0) {
            // Discounts apply to compute costs only (EC2/Fargate), not storage/licenses
            double discountableBase = resp.ec2NodesCost + resp.fargateCost;
            resp.discountSaving = discountableBase * maxDiscountPct / 100.0;
            total -= resp.discountSaving;
            resp.lineItems.put("Discount Applied",
                    String.format("%.0f%% on compute ($%.2f base) = -$%.2f/mo",
                            maxDiscountPct, discountableBase, resp.discountSaving));
        }

        resp.totalMonthlyContainerTco = Math.round(total * 100.0) / 100.0;
        resp.note = "All costs based on AWS us-east-1 on-demand pricing. "
                + "RI/Spot/EDP discounts applied to compute only. "
                + "EBS priced at gp3 rates. ALB at standard LCU pricing.";

        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       HELPER: Fetch EC2 on-demand prices from AWS Pricing API (SDK v2)
    ================================================================ */
    private void enrichEc2Prices(Map<String, Double> prices) {
        try (PricingClient pricingClient = PricingClient.builder()
                .region(Region.US_EAST_1)
                .build()) {

            List<String> instanceTypes = new ArrayList<>(prices.keySet());
            for (String instanceType : instanceTypes) {
                try {
                    GetProductsRequest req = GetProductsRequest.builder()
                            .serviceCode("AmazonEC2")
                            .filters(
                                    Filter.builder().type("TERM_MATCH").field("instanceType").value(instanceType).build(),
                                    Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                                    Filter.builder().type("TERM_MATCH").field("operatingSystem").value("Linux").build(),
                                    Filter.builder().type("TERM_MATCH").field("tenancy").value("Shared").build(),
                                    Filter.builder().type("TERM_MATCH").field("preInstalledSw").value("NA").build(),
                                    Filter.builder().type("TERM_MATCH").field("capacitystatus").value("Used").build()
                            )
                            .maxResults(1)
                            .formatVersion("aws_v1")
                            .build();

                    GetProductsResponse result = pricingClient.getProducts(req);
                    log.info("GetProductsResponse" + result);
                    JSONLogger.logAsJSON(log, result);
                    if (!result.priceList().isEmpty()) {
                        String priceJson = result.priceList().getFirst();
                        JsonNode root = mapper.readTree(priceJson);
                        double price = extractOnDemandPriceFromJson(root);
                        if (price > 0) prices.put(instanceType, price);
                    }
                } catch (Exception e) {
                    log.severe("Could not fetch price for " + instanceType +": " + e.getMessage());
                }
            }
        }
    }

    private double fetchEc2Price(String instanceType) {
        if (EC2_FALLBACK_PRICES.containsKey(instanceType)) {
            return EC2_FALLBACK_PRICES.get(instanceType);
        }
        try {
            Map<String, Double> tmp = new HashMap<>();
            tmp.put(instanceType, 0.0);
            enrichEc2Prices(tmp);
            return tmp.getOrDefault(instanceType, 0.096); // default m6i.large
        } catch (Exception ignored) {}
        return 0.096; // fallback
    }

    /* ================================================================
       HELPER: Fetch Fargate prices from AWS Pricing API (SDK v2)
    ================================================================ */
    @PostConstruct
    private double[] fetchFargatePrices() throws Exception {
        try (PricingClient pricingClient = PricingClient.builder()
                .region(Region.US_EAST_1)
                .build()) {

            double vcpuPrice = FARGATE_VCPU_PER_HOUR;
            double gbPrice   = FARGATE_GB_PER_HOUR;

            // Fargate vCPU
            GetProductsRequest vcpuReq = GetProductsRequest.builder()
                    .serviceCode("AmazonECS")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("usagetype").value("USE1-Fargate-vCPU-Hours:perCPU").build()
                    )
                    .maxResults(1)
                    .formatVersion("aws_v1")
                    .build();

            GetProductsResponse vcpuResult = pricingClient.getProducts(vcpuReq);
            log.info("GetProductsResponse" + vcpuResult);
            JSONLogger.logAsJSON(log, vcpuResult);
            if (!vcpuResult.priceList().isEmpty()) {
                double p = extractOnDemandPriceFromJson(mapper.readTree(vcpuResult.priceList().getFirst()));
                if (p > 0) vcpuPrice = p;
            }

            // Fargate GB
            GetProductsRequest gbReq = GetProductsRequest.builder()
                    .serviceCode("AmazonECS")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("usagetype").value("USE1-Fargate-GB-Hours").build()
                    )
                    .maxResults(1)
                    .formatVersion("aws_v1")
                    .build();

            var gbResult = pricingClient.getProducts(gbReq);
            if (!gbResult.priceList().isEmpty()) {
                double p = extractOnDemandPriceFromJson(mapper.readTree(gbResult.priceList().getFirst()));
                if (p > 0) gbPrice = p;
            }

            return new double[]{vcpuPrice, gbPrice};
        }
    }

    /* ================================================================
       HELPER: Fetch API Gateway prices from AWS Pricing API (SDK v2)
    ================================================================ */
    private Map<String, Double> fetchApiGatewayPrices() throws Exception {
        try (PricingClient pricingClient = PricingClient.builder()
                .region(Region.US_EAST_1)
                .build()) {

            Map<String, Double> prices = new HashMap<>();

            GetProductsRequest restReq = GetProductsRequest.builder()
                    .serviceCode("AmazonApiGateway")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("usagetype").value("USE1-ApiGatewayRequest").build()
                    )
                    .maxResults(1)
                    .formatVersion("aws_v1")
                    .build();

            GetProductsResponse restResult = pricingClient.getProducts(restReq);
            log.info("GetProductsResponse" + restResult);
            JSONLogger.logAsJSON(log, restResult);

            if (!restResult.priceList().isEmpty()) {
                double p = extractOnDemandPriceFromJson(mapper.readTree(restResult.priceList().getFirst()));
                if (p > 0) prices.put("rest-per-1m", p * 1_000_000); // per-request → per-million
            }

            return prices;
        }
    }

    /* ================================================================
       HELPER: Extract on-demand USD price from AWS Pricing JSON (unchanged)
    ================================================================ */
    private double extractOnDemandPriceFromJson(JsonNode root) {
        try {
            JsonNode terms = root.path("terms").path("OnDemand");
            Iterator<JsonNode> termIter = terms.elements();
            if (!termIter.hasNext()) return -1;
            JsonNode term = termIter.next();
            JsonNode priceDims = term.path("priceDimensions");
            Iterator<JsonNode> dimIter = priceDims.elements();
            if (!dimIter.hasNext()) return -1;
            JsonNode dim = dimIter.next();
            String usdPrice = dim.path("pricePerUnit").path("USD").asText("0");
            return Double.parseDouble(usdPrice);
        } catch (Exception e) {
            log.severe("Could not parse price from JSON: " + e.getMessage());
            return -1;
        }
    }

}