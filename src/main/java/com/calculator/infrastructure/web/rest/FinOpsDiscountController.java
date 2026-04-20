package com.calculator.infrastructure.web.rest;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;

import java.util.*;

import static com.calculator.application.services.utils.MathUtils.round2;
import static com.calculator.application.services.utils.MathUtils.round4;

@RestController
@RequestMapping("/api/finops")
@CrossOrigin(origins = "*")
public class FinOpsDiscountController {

    private static final Logger log = LoggerFactory.getLogger(FinOpsDiscountController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    /* ── Fallback discount rates (Q1-2025) if Pricing API unavailable ── */
    private static final double RI_1YR_STD_DISCOUNT_PCT       = 38.0; /* Standard RI 1-yr, no upfront */
    private static final double RI_3YR_STD_DISCOUNT_PCT       = 57.0; /* Standard RI 3-yr, no upfront */
    private static final double RI_1YR_CONV_DISCOUNT_PCT      = 31.0; /* Convertible RI 1-yr */
    private static final double RI_3YR_CONV_DISCOUNT_PCT      = 54.0; /* Convertible RI 3-yr */
    public static final double SP_COMPUTE_1YR_DISCOUNT_PCT   = 36.0; /* Compute Savings Plan 1-yr */
    public static final double SP_COMPUTE_3YR_DISCOUNT_PCT   = 52.0; /* Compute Savings Plan 3-yr */
    private static final double SP_EC2_1YR_DISCOUNT_PCT       = 40.0; /* EC2 Instance Savings Plan 1-yr */
    private static final double SP_EC2_3YR_DISCOUNT_PCT       = 60.0; /* EC2 Instance Savings Plan 3-yr */
    public static final double FARGATE_SP_1YR_DISCOUNT_PCT   = 20.0; /* Fargate Savings Plan 1-yr */
    public static final double FARGATE_SP_3YR_DISCOUNT_PCT   = 37.0; /* Fargate Savings Plan 3-yr */

    /* ================================================================
       REQUEST / RESPONSE DTOs
    ================================================================ */
    public static class DiscountRequest {
        @JsonProperty public String ec2InstanceType      = "m6i.large";
        @JsonProperty public int    ec2ReplicaCount      = 1;
        @JsonProperty public int    fargateActivePods    = 0;
        @JsonProperty public double fargateVcpuPerPod    = 0.25;
        @JsonProperty public double fargateGbPerPod      = 0.5;
        @JsonProperty public boolean riStandard1yr       = false;
        @JsonProperty public boolean riStandard3yr       = false;
        @JsonProperty public boolean riConvertible1yr    = false;
        @JsonProperty public boolean savingsPlan1yr      = false;
        @JsonProperty public boolean savingsPlan3yr      = false;
        @JsonProperty public boolean fargateSavingsPlan  = false;
        /** EKS control plane cost to include in baseline */
        @JsonProperty public double eksMonthlyFixed      = 0.0;
    }

    public static class DiscountResponse {
        @JsonProperty public double baselineOnDemandPerMonth;
        @JsonProperty public double bestDiscountPct;
        @JsonProperty public double bestDiscountedCost;
        @JsonProperty public double bestMonthlySaving;
        @JsonProperty public String bestStrategy;
        @JsonProperty public List<DiscountOption> options = new ArrayList<>();
        @JsonProperty public double ec2OnDemandPricePerHour;
        @JsonProperty public double fargateOnDemandCostMonthly;
        @JsonProperty public String ec2InstanceType;
        @JsonProperty public int    ec2ReplicaCount;
        @JsonProperty public String source;
        @JsonProperty public String note;
    }

    public static class DiscountOption {
        @JsonProperty public String strategy;
        @JsonProperty public double discountPct;
        @JsonProperty public double monthlyOnDemand;
        @JsonProperty public double monthlyDiscounted;
        @JsonProperty public double monthlySaving;
        @JsonProperty public double annualSaving;
        @JsonProperty public String description;
        @JsonProperty public boolean livePrice; /* true if from AWS Pricing API, false if fallback */
    }

    public static class InstanceRiPrices {
        @JsonProperty public String instanceType;
        @JsonProperty public double onDemandPerHour;
        @JsonProperty public double ri1yrNoUpfrontPerHour;
        @JsonProperty public double ri3yrNoUpfrontPerHour;
        @JsonProperty public double ri1yrActualDiscountPct;
        @JsonProperty public double ri3yrActualDiscountPct;
        @JsonProperty public String source;
    }

    /* ================================================================
       ENDPOINT 1: POST /api/finops/discount
    ================================================================ */
    @PostMapping("/discount")
    public ResponseEntity<DiscountResponse> computeDiscount(
            @RequestBody DiscountRequest req) {

        DiscountResponse resp = new DiscountResponse();
        resp.ec2InstanceType = req.ec2InstanceType;
        resp.ec2ReplicaCount = req.ec2ReplicaCount;

        /* ── Step 1: Get EC2 on-demand price ── */
        double ec2OdPerHour = fetchInstanceOnDemand(req.ec2InstanceType);
        resp.ec2OnDemandPricePerHour = round4(ec2OdPerHour);

        /* ── Step 2: Fargate on-demand cost ── */
        double fargateOd = 0;
        if (req.fargateActivePods > 0) {
            fargateOd = (req.fargateVcpuPerPod * 0.04048 + req.fargateGbPerPod * 0.004445)
                    * req.fargateActivePods * 730;
        }
        resp.fargateOnDemandCostMonthly = round2(fargateOd);

        double ec2Baseline   = ec2OdPerHour * 730 * req.ec2ReplicaCount;
        double totalBaseline = ec2Baseline + fargateOd + req.eksMonthlyFixed;
        resp.baselineOnDemandPerMonth = round2(totalBaseline);

        /* ── Step 3: Build discount options ── */
        InstanceRiPrices riPrices = fetchRiPrices(req.ec2InstanceType);

        if (req.riStandard1yr) {
            double pct    = riPrices != null ? riPrices.ri1yrActualDiscountPct : RI_1YR_STD_DISCOUNT_PCT;
            boolean live  = riPrices != null;
            addOption(resp.options, "Standard RI (1-yr, no upfront)", pct,
                    ec2Baseline, totalBaseline,
                    "Committed 1-year term for the selected EC2 instance type. "
                            + "No upfront payment. Best for predictable, steady-state workloads.", live);
        }
        if (req.riStandard3yr) {
            double pct   = riPrices != null ? riPrices.ri3yrActualDiscountPct : RI_3YR_STD_DISCOUNT_PCT;
            boolean live = riPrices != null;
            addOption(resp.options, "Standard RI (3-yr, no upfront)", pct,
                    ec2Baseline, totalBaseline,
                    "3-year commitment for maximum EC2 discount. Instance family locked. "
                            + "Highest saving but least flexible.", live);
        }
        if (req.riConvertible1yr) {
            addOption(resp.options, "Convertible RI (1-yr)", RI_1YR_CONV_DISCOUNT_PCT,
                    ec2Baseline, totalBaseline,
                    "1-yr RI that can be exchanged for a different instance family. "
                            + "~7% less savings than Standard RI but allows instance type changes.", false);
        }
        if (req.savingsPlan1yr) {
            double computeSaving = ec2Baseline * SP_COMPUTE_1YR_DISCOUNT_PCT / 100;
            double fargateSaving = fargateOd * FARGATE_SP_1YR_DISCOUNT_PCT / 100;
            double totalSaving   = computeSaving + fargateSaving;
            DiscountOption opt   = new DiscountOption();
            opt.strategy         = "Compute Savings Plan (1-yr)";
            opt.discountPct      = round2(totalSaving / totalBaseline * 100);
            opt.monthlyOnDemand  = round2(totalBaseline);
            opt.monthlyDiscounted= round2(totalBaseline - totalSaving);
            opt.monthlySaving    = round2(totalSaving);
            opt.annualSaving     = round2(totalSaving * 12);
            opt.livePrice        = false;
            opt.description      = "Flexible commitment covering EC2 (any family, region) + Fargate + Lambda. "
                    + "EC2 saving: ~" + SP_COMPUTE_1YR_DISCOUNT_PCT + "%. "
                    + "Fargate saving: ~" + FARGATE_SP_1YR_DISCOUNT_PCT + "%. "
                    + "Recommended for multi-family or frequently-changing architectures.";
            resp.options.add(opt);
        }
        if (req.savingsPlan3yr) {
            double computeSaving = ec2Baseline * SP_COMPUTE_3YR_DISCOUNT_PCT / 100;
            double fargateSaving = fargateOd * FARGATE_SP_3YR_DISCOUNT_PCT / 100;
            double totalSaving   = computeSaving + fargateSaving;
            DiscountOption opt   = new DiscountOption();
            opt.strategy         = "Compute Savings Plan (3-yr)";
            opt.discountPct      = round2(totalSaving / totalBaseline * 100);
            opt.monthlyOnDemand  = round2(totalBaseline);
            opt.monthlyDiscounted= round2(totalBaseline - totalSaving);
            opt.monthlySaving    = round2(totalSaving);
            opt.annualSaving     = round2(totalSaving * 12);
            opt.livePrice        = false;
            opt.description      = "3-yr compute savings plan covering EC2 + Fargate + Lambda. "
                    + "Highest flexibility, significant multi-service savings.";
            resp.options.add(opt);
        }

        /* ── Step 4: Pick best strategy ── */
        DiscountOption best = resp.options.stream()
                .max(Comparator.comparingDouble(o -> o.monthlySaving))
                .orElse(null);
        if (best != null) {
            resp.bestDiscountPct     = best.discountPct;
            resp.bestDiscountedCost  = best.monthlyDiscounted;
            resp.bestMonthlySaving   = best.monthlySaving;
            resp.bestStrategy        = best.strategy;
        }

        resp.source = (riPrices != null) ? "AWS Pricing API (live)" : "Fallback rates (Q1-2025)";
        resp.note   = "RI discounts apply to EC2 compute costs only (not storage, data transfer, or support). "
                + "Savings Plans apply to EC2 + Fargate + Lambda compute. "
                + "EKS control plane ($73/mo) is not discountable.";

        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       ENDPOINT 2: GET /api/finops/ri-prices/{instanceType}
    ================================================================ */
    @GetMapping("/ri-prices/{instanceType}")
    public ResponseEntity<InstanceRiPrices> getRiPrices(
            @PathVariable String instanceType) {
        InstanceRiPrices prices = fetchRiPrices(instanceType);
        if (prices == null) {
            prices = new InstanceRiPrices();
            prices.instanceType            = instanceType;
            prices.onDemandPerHour         = fetchInstanceOnDemand(instanceType);
            prices.ri1yrNoUpfrontPerHour   = round4(prices.onDemandPerHour * (1 - RI_1YR_STD_DISCOUNT_PCT/100));
            prices.ri3yrNoUpfrontPerHour   = round4(prices.onDemandPerHour * (1 - RI_3YR_STD_DISCOUNT_PCT/100));
            prices.ri1yrActualDiscountPct  = RI_1YR_STD_DISCOUNT_PCT;
            prices.ri3yrActualDiscountPct  = RI_3YR_STD_DISCOUNT_PCT;
            prices.source                  = "Fallback rates (Q1-2025)";
        }
        return ResponseEntity.ok(prices);
    }

    /* ================================================================
       HELPERS
    ================================================================ */
    private void addOption(List<DiscountOption> opts, String strategy,
                           double discountPct, double discountableBase, double totalBase,
                           String description, boolean livePrice) {
        double saving = discountableBase * discountPct / 100;
        DiscountOption o = new DiscountOption();
        o.strategy          = strategy;
        o.discountPct       = round2(discountPct);
        o.monthlyOnDemand   = round2(totalBase);
        o.monthlyDiscounted = round2(totalBase - saving);
        o.monthlySaving     = round2(saving);
        o.annualSaving      = round2(saving * 12);
        o.livePrice         = livePrice;
        o.description       = description;
        opts.add(o);
    }

    private double fetchInstanceOnDemand(String instanceType) {
        Map<String, Double> fallbacks = new HashMap<>();
        fallbacks.put("t3.micro",  0.0104); fallbacks.put("t3.small",  0.0208);
        fallbacks.put("t3.medium", 0.0416); fallbacks.put("t3.large",  0.0832);
        fallbacks.put("m6i.large", 0.096);  fallbacks.put("m6i.xlarge",0.192);
        fallbacks.put("m6i.2xlarge",0.384); fallbacks.put("c6i.large", 0.085);
        fallbacks.put("c6i.xlarge",0.170);  fallbacks.put("m6g.large", 0.077);
        fallbacks.put("m6g.xlarge",0.154);  fallbacks.put("r6g.large", 0.1008);
        try (PricingClient pc = PricingClient.builder().region(Region.US_EAST_1).build()) {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("instanceType").value(instanceType).build(),
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("operatingSystem").value("Linux").build(),
                            Filter.builder().type("TERM_MATCH").field("tenancy").value("Shared").build(),
                            Filter.builder().type("TERM_MATCH").field("preInstalledSw").value("NA").build(),
                            Filter.builder().type("TERM_MATCH").field("capacitystatus").value("Used").build()
                    ).maxResults(1).formatVersion("aws_v1").build();
            var result = pc.getProducts(req);
            if (!result.priceList().isEmpty()) {
                double p = extractUsdPrice(mapper.readTree(result.priceList().get(0)), "OnDemand");
                if (p > 0) return p;
            }
        } catch (Exception e) {
            log.warn("Could not fetch on-demand price for {}: {}", instanceType, e.getMessage());
        }
        return fallbacks.getOrDefault(instanceType, 0.096);
    }

    private InstanceRiPrices fetchRiPrices(String instanceType) {
        try (PricingClient pc = PricingClient.builder().region(Region.US_EAST_1).build()) {
            /* On-demand */
            double od = fetchInstanceOnDemand(instanceType);

            /* 1-yr No Upfront */
            double ri1yr = fetchReservedPrice(pc, instanceType, "1yr", "No Upfront");
            /* 3-yr No Upfront */
            double ri3yr = fetchReservedPrice(pc, instanceType, "3yr", "No Upfront");

            if (ri1yr <= 0 && ri3yr <= 0) return null; /* Pricing API returned nothing */

            InstanceRiPrices p = new InstanceRiPrices();
            p.instanceType             = instanceType;
            p.onDemandPerHour          = round4(od);
            p.ri1yrNoUpfrontPerHour    = ri1yr > 0 ? round4(ri1yr) : round4(od * (1 - RI_1YR_STD_DISCOUNT_PCT/100));
            p.ri3yrNoUpfrontPerHour    = ri3yr > 0 ? round4(ri3yr) : round4(od * (1 - RI_3YR_STD_DISCOUNT_PCT/100));
            p.ri1yrActualDiscountPct   = od > 0 ? round2((1 - p.ri1yrNoUpfrontPerHour / od) * 100) : RI_1YR_STD_DISCOUNT_PCT;
            p.ri3yrActualDiscountPct   = od > 0 ? round2((1 - p.ri3yrNoUpfrontPerHour / od) * 100) : RI_3YR_STD_DISCOUNT_PCT;
            p.source = "AWS Pricing API (live)";
            return p;
        } catch (Exception e) {
            log.warn("Could not fetch RI prices for {}: {}", instanceType, e.getMessage());
            return null;
        }
    }

    private double fetchReservedPrice(PricingClient pc, String instanceType,
                                      String leaseContractLength, String purchaseOption) {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("instanceType").value(instanceType).build(),
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("operatingSystem").value("Linux").build(),
                            Filter.builder().type("TERM_MATCH").field("tenancy").value("Shared").build(),
                            Filter.builder().type("TERM_MATCH").field("preInstalledSw").value("NA").build(),
                            Filter.builder().type("TERM_MATCH").field("offeringClass").value("standard").build(),
                            Filter.builder().type("TERM_MATCH").field("leaseContractLength").value(leaseContractLength).build(),
                            Filter.builder().type("TERM_MATCH").field("purchaseOption").value(purchaseOption).build()
                    ).maxResults(1).formatVersion("aws_v1").build();
            var result = pc.getProducts(req);
            if (!result.priceList().isEmpty()) {
                return extractUsdPrice(mapper.readTree(result.priceList().get(0)), "Reserved");
            }
        } catch (Exception e) {
            log.debug("RI price fetch failed for {} {}-{}: {}", instanceType, leaseContractLength, purchaseOption, e.getMessage());
        }
        return -1;
    }

    private double extractUsdPrice(JsonNode root, String termType) {
        try {
            JsonNode terms = root.path("terms").path(termType);
            Iterator<JsonNode> termIter = terms.elements();
            if (!termIter.hasNext()) return -1;
            JsonNode term = termIter.next();
            JsonNode dims = term.path("priceDimensions");
            Iterator<JsonNode> dimIter = dims.elements();
            if (!dimIter.hasNext()) return -1;
            JsonNode dim = dimIter.next();
            String usd = dim.path("pricePerUnit").path("USD").asText("0");
            return Double.parseDouble(usd);
        } catch (Exception e) {
            return -1;
        }
    }

}