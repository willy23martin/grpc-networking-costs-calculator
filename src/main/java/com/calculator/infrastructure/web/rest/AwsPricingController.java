package com.calculator.infrastructure.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.*;

import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/aws")
public class AwsPricingController {

    private static final Logger log = Logger.getLogger(AwsPricingController.class.getName());
    private static final Region PRICING_REGION = Region.US_EAST_1;
    private static final String LOCATION = "US East (N. Virginia)";

    private final PricingClient pricing;
    private final ObjectMapper  mapper  = new ObjectMapper();

    public AwsPricingController() {
        this.pricing = PricingClient.builder()
                .region(PRICING_REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    // ── EC2 Instance Types ───────────────────────────────────────────────────

    /**
     * Returns a curated list of EC2 instance types enriched with:
     *  - vCPU count, memory (GiB), network bandwidth (Gbps)
     *  - on-demand hourly price in USD (Linux, us-east-1)
     *
     * The UI uses this to populate the instance-type selector and compute
     * the replica sizing formula C_in / C_out values.
     */
    @GetMapping("/ec2-instances")
    public List<Map<String, Object>> getEc2Instances() {
        // Curated families relevant for gRPC workloads
        List<String> families = List.of(
                "t3.micro","t3.small","t3.medium","t3.large",
                "m6i.large","m6i.xlarge","m6i.2xlarge","m6i.4xlarge","m6i.8xlarge",
                "c6i.large","c6i.xlarge","c6i.2xlarge","c6i.4xlarge","c6i.8xlarge",
                "r6i.large","r6i.xlarge","r6i.2xlarge","r6i.4xlarge"
        );

        // Network bandwidth map (Gbps) — from AWS published specs
        Map<String,Double> networkGbps = Map.ofEntries(
                Map.entry("t3.micro",   0.5), Map.entry("t3.small",   0.5),
                Map.entry("t3.medium",  0.5), Map.entry("t3.large",   0.5),
                Map.entry("m6i.large",  12.5), Map.entry("m6i.xlarge",  12.5),
                Map.entry("m6i.2xlarge",12.5), Map.entry("m6i.4xlarge",  25.0),
                Map.entry("m6i.8xlarge",25.0),
                Map.entry("c6i.large",  12.5), Map.entry("c6i.xlarge",  12.5),
                Map.entry("c6i.2xlarge",12.5), Map.entry("c6i.4xlarge",  25.0),
                Map.entry("c6i.8xlarge",25.0),
                Map.entry("r6i.large",  12.5), Map.entry("r6i.xlarge",  12.5),
                Map.entry("r6i.2xlarge",12.5), Map.entry("r6i.4xlarge",  25.0)
        );

        List<Map<String, Object>> result = new ArrayList<>();
        for (String instanceType : families) {
            try {
                double price  = fetchEc2OnDemandPrice(instanceType);
                double bwGbps = networkGbps.getOrDefault(instanceType, 1.0);
                long   bwBytes= (long)(bwGbps * 1_000_000_000 / 8); // bytes/sec

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("instanceType", instanceType);
                entry.put("networkGbps",  bwGbps);
                entry.put("networkBytesPerSec", bwBytes);
                entry.put("pricePerHourUsd", price);
                entry.put("pricePerMonthUsd", Math.round(price * 730 * 100.0) / 100.0);
                result.add(entry);
            } catch (Exception e) {
                log.warning("Failed to fetch price for " + instanceType + ": " + e.getMessage());
            }
        }
        return result;
    }

    private double fetchEc2OnDemandPrice(String instanceType) {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value(instanceType).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("operatingSystem").value("Linux").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("tenancy").value("Shared").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("preInstalledSw").value("NA").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("capacitystatus").value("Used").build()
                    )
                    .formatVersion("aws_v1")
                    .maxResults(1)
                    .build();
            GetProductsResponse resp = pricing.getProducts(req);
            if (resp.priceList().isEmpty()) return 0.0;
            JsonNode root = mapper.readTree(resp.priceList().get(0));
            return root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(0.0);
        } catch (Exception e) {
            // Fallback table for offline/test environments
            Map<String,Double> fallback = Map.of(
                    "t3.micro",0.0104, "t3.small",0.0208, "t3.medium",0.0416,
                    "m6i.large",0.096, "m6i.xlarge",0.192, "c6i.large",0.085,
                    "r6i.large",0.126
            );
            return fallback.getOrDefault(instanceType, 0.10);
        }
    }

    // ── ALB Pricing ─────────────────────────────────────────────────────────

    @GetMapping("/alb-pricing")
    public Map<String, Object> getAlbPricing() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // ALB fixed charge: $0.008/hr per ALB
            // LCU charge: $0.008/LCU-hr  (1 LCU = 25 new connections/s OR 3000 active connections
            //              OR 1 GB/hr processed OR 1000 rule evaluations/s)
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AWSElasticLoadBalancing")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("group").value("ALB:LoadBalancer").build()
                    )
                    .formatVersion("aws_v1").maxResults(10).build();
            GetProductsResponse resp = pricing.getProducts(req);
            double fixedPerHour = 0.0, lcuPerHour = 0.0;
            for (String p : resp.priceList()) {
                JsonNode root = mapper.readTree(p);
                String usageType = root.path("product").path("attributes").path("usagetype").asText("");
                double price = root.path("terms").path("OnDemand").fields().next()
                        .getValue().path("priceDimensions").fields().next()
                        .getValue().path("pricePerUnit").path("USD").asDouble(0.0);
                if (usageType.contains("LoadBalancerUsage") && fixedPerHour == 0) fixedPerHour = price;
                if (usageType.contains("LCUUsage") && lcuPerHour == 0) lcuPerHour = price;
            }
            result.put("fixedPerHourUsd",   fixedPerHour > 0 ? fixedPerHour : 0.008);
            result.put("lcuPerHourUsd",     lcuPerHour   > 0 ? lcuPerHour   : 0.008);
            result.put("fixedPerMonthUsd",  Math.round((fixedPerHour > 0 ? fixedPerHour : 0.008) * 730 * 100) / 100.0);
            result.put("lcuPerMonthBase",   Math.round((lcuPerHour   > 0 ? lcuPerHour   : 0.008) * 730 * 100) / 100.0);
            result.put("source", "AWS Pricing API");
        } catch (Exception e) {
            log.warning("ALB pricing fetch failed: " + e.getMessage());
            result.put("fixedPerHourUsd",  0.008);
            result.put("lcuPerHourUsd",    0.008);
            result.put("fixedPerMonthUsd", 5.84);
            result.put("lcuPerMonthBase",  5.84);
            result.put("source", "fallback");
        }
        return result;
    }

    // ── Database Backup & Recovery ───────────────────────────────────────────

    @GetMapping("/database-backup-pricing")
    public Map<String, Object> getDatabaseBackupPricing() {
        Map<String, Object> result = new LinkedHashMap<>();

        // S3 Standard storage for backups
        result.put("s3StandardPerGbMonth",   fetchSimplePrice("AmazonS3",  "S3 Standard", "Storage", 0.023));
        // RDS Snapshot storage
        result.put("rdsSnapshotPerGbMonth",  fetchSimplePrice("AmazonRDS", "RDS Snapshot", "Database Storage", 0.095));
        // RDS Multi-AZ surcharge (approx 2× Single-AZ)
        result.put("rdsMultiAzSurchargeNote","Multi-AZ roughly doubles the RDS instance cost. Select instance above to compute.");
        // Aurora replica — per replica per hour (us-east-1, db.r6g.large as reference)
        result.put("auroraReplicaPerHour",   fetchAuroraReplicaPrice());
        // DynamoDB Global Tables — replicated write cost $0.000975/WRU additional per region
        result.put("dynamoGlobalTablePerWruUsd", 0.000975);
        result.put("dynamoGlobalTableNote",  "Add ~$0.000975/WRU per extra replication region beyond the primary.");
        return result;
    }

    private double fetchAuroraReplicaPrice() {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonRDS")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("databaseEngine").value("Aurora MySQL").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value("db.r6g.large").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("deploymentOption").value("Multi-AZ").build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();
            GetProductsResponse resp = pricing.getProducts(req);
            if (resp.priceList().isEmpty()) return 0.26;
            JsonNode root = mapper.readTree(resp.priceList().get(0));
            return root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(0.26);
        } catch (Exception e) { return 0.26; }
    }

    // ── Security Services ────────────────────────────────────────────────────

    @GetMapping("/security-services")
    public Map<String, Object> getSecurityServicesPricing() {
        Map<String, Object> result = new LinkedHashMap<>();

        // GuardDuty — per GB of CloudTrail/VPC flow logs analysed
        result.put("guardDutyPerGbLogs",       fetchSimplePrice("AmazonGuardDuty", "Logs", "Security", 1.00));
        result.put("guardDutyFirstGbFreeNote", "First 500 GB/month free. $1.00/GB thereafter (tiered).");

        // Amazon Inspector — per EC2 instance
        result.put("inspectorPerInstanceMonth", fetchSimplePrice("AmazonInspector", "EC2 Instance", "Security", 1.178));

        // AWS WAF — per WebACL + per rule + per million requests
        result.put("wafWebAclPerMonth",         fetchSimplePrice("awswaf", "WebACL", "Security", 5.00));
        result.put("wafRulePerMonth",            fetchSimplePrice("awswaf", "Rule",   "Security", 1.00));
        result.put("wafPer1MRequests",           fetchSimplePrice("awswaf", "Request","Security", 0.60));

        // Macie — per GB of S3 data classified
        result.put("maciePerGbClassified",      fetchSimplePrice("AmazonMacie", "Data Classification", "Security", 1.00));
        result.put("macieFirstGbFreeNote",      "First 1 GB/month free. $1.00/GB thereafter.");

        // CloudWatch Logs — ingestion + storage
        result.put("cloudwatchLogsIngestionPerGb", 0.50);
        result.put("cloudwatchLogsStoragePerGbMonth", 0.03);

        // AWS Audit Manager — per assessment
        result.put("auditManagerPerAssessmentMonth", fetchSimplePrice("AWSAuditManager","Assessment","Security",6.00));

        // KMS — CMK per month + per 10k API calls
        result.put("kmsCmkPerMonth",            1.00);
        result.put("kmsApiCallsPer10k",         0.03);
        result.put("kmsNote",                   "Data encryption at rest via KMS. $1/CMK/month + $0.03 per 10,000 API calls.");

        return result;
    }

    // ── Cost Optimisation ────────────────────────────────────────────────────

    @GetMapping("/cost-optimisation")
    public Map<String, Object> getCostOptimisationPricing() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Reserved Instance savings vs On-Demand (typical 1-yr no upfront)
        result.put("reservedInstance1yrSavingsPct",  36);
        result.put("reservedInstance3yrSavingsPct",  57);
        result.put("convertibleRi1yrSavingsPct",     28);
        result.put("convertibleRi3yrSavingsPct",     47);
        result.put("riNote","Standard RIs offer the highest discount but cannot be exchanged. Convertible RIs can be exchanged for different instance families.");

        // Compute Savings Plans (covers EC2 + Lambda + Fargate)
        result.put("savingsPlan1yrSavingsPct", 31);
        result.put("savingsPlan3yrSavingsPct", 50);
        result.put("savingsPlanNote","Savings Plans apply automatically to the highest compute usage. Commitment is $/hour not to a specific instance type.");

        // EC2 Instance Savings Plans
        result.put("ec2SavingsPlan1yrPct", 36);
        result.put("ec2SavingsPlan3yrPct", 57);

        // Trusted Advisor — available with Business/Enterprise Support
        result.put("trustedAdvisorNote","Trusted Advisor cost optimisation checks (idle resources, RI recommendations) require AWS Business or Enterprise Support ($100+/mo or 10% of monthly usage).");
        result.put("businessSupportMinMonthUsd", 100);
        result.put("businessSupportPctMonthlyUsage", 10);

        return result;
    }

    // ── Caching ─────────────────────────────────────────────────────────────

    @GetMapping("/caching-pricing")
    public Map<String, Object> getCachingPricing() {
        Map<String, Object> result = new LinkedHashMap<>();
        // ElastiCache Redis — cache.r6g.large, us-east-1 on-demand
        result.put("redisR6gLargePerHour",       fetchElastiCachePrice("cache.r6g.large",   "redis"));
        result.put("redisR6gXlargePerHour",      fetchElastiCachePrice("cache.r6g.xlarge",  "redis"));
        result.put("redisR6g2xlargePerHour",     fetchElastiCachePrice("cache.r6g.2xlarge", "redis"));
        // ElastiCache Memcached
        result.put("memcachedR6gLargePerHour",   fetchElastiCachePrice("cache.r6g.large",   "memcached"));
        result.put("memcachedR6gXlargePerHour",  fetchElastiCachePrice("cache.r6g.xlarge",  "memcached"));
        // Backup storage for Redis snapshots — same as S3 standard
        result.put("snapshotStoragePerGbMonth", 0.085);
        result.put("note","ElastiCache on-demand prices shown. Reserved Nodes offer up to 55% savings (1yr) or 70% (3yr) for committed workloads.");
        return result;
    }

    private double fetchElastiCachePrice(String nodeType, String engine) {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonElastiCache")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("cacheEngine").value(engine).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value(nodeType).build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();
            GetProductsResponse resp = pricing.getProducts(req);
            if (resp.priceList().isEmpty()) return 0.0;
            JsonNode root = mapper.readTree(resp.priceList().get(0));
            return root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(0.0);
        } catch (Exception e) {
            Map<String,Double> fb = Map.of("cache.r6g.large",0.166,"cache.r6g.xlarge",0.332,"cache.r6g.2xlarge",0.665);
            return fb.getOrDefault(nodeType, 0.20);
        }
    }

    // ── Shared helper ────────────────────────────────────────────────────────

    private double fetchSimplePrice(String serviceCode, String productFamily,
                                    String group, double fallback) {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode(serviceCode)
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("productFamily").value(productFamily).build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();
            GetProductsResponse resp = pricing.getProducts(req);
            if (resp.priceList().isEmpty()) return fallback;
            JsonNode root = mapper.readTree(resp.priceList().get(0));
            double price = root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(fallback);
            return price > 0 ? price : fallback;
        } catch (Exception e) { return fallback; }
    }
}