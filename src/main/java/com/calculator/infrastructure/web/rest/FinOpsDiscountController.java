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
public class FinOpsDiscountController { // TODO

    private static final Logger log = LoggerFactory.getLogger(FinOpsDiscountController.class);
    private final ObjectMapper mapper = new ObjectMapper();

    /* ── Fallback discount rates (Q1-2025) if Pricing API unavailable ── */
    private static final double RI_1YR_STD_DISCOUNT_PCT       = 38.0; /* Standard RI 1-yr, no upfront */
    private static final double RI_3YR_STD_DISCOUNT_PCT       = 57.0; /* Standard RI 3-yr, no upfront */
    private static final double RI_CONVERTIBLE_1YR_DISCOUNT_PCT = 31.0; /* Convertible RI 1-yr, no upfront */
    private static final double SAVINGS_PLAN_1YR_DISCOUNT_PCT = 29.0; /* Compute SP 1-yr, no upfront */
    private static final double SAVINGS_PLAN_3YR_DISCOUNT_PCT = 48.0; /* Compute SP 3-yr, no upfront */

    @GetMapping("/ri-prices/{instanceType}")
    public ResponseEntity<Map<String, Object>> getRiPrices(@PathVariable String instanceType) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("instanceType", instanceType);

        double onDemandPrice = getOnDemandPrice(instanceType);

        if (onDemandPrice <= 0) {
            // Fallback pricing strategies based on typical AWS values if AWS pricing service is offline
            double fallbackOnDemand = 0.096; // Baseline approx for m6i.large
            response.put("source", "Local Fallback Estimates (API Down)");
            response.put("onDemandPerHourUsd", fallbackOnDemand);
            response.put("riStandard1yrPerHourUsd", round4(fallbackOnDemand * (1 - RI_1YR_STD_DISCOUNT_PCT / 100.0)));
            response.put("riStandard3yrPerHourUsd", round4(fallbackOnDemand * (1 - RI_3YR_STD_DISCOUNT_PCT / 100.0)));
            response.put("riConvertible1yrPerHourUsd", round4(fallbackOnDemand * (1 - RI_CONVERTIBLE_1YR_DISCOUNT_PCT / 100.0)));
            response.put("savingsPlan1yrPerHourUsd", round4(fallbackOnDemand * (1 - SAVINGS_PLAN_1YR_DISCOUNT_PCT / 100.0)));
            response.put("savingsPlan3yrPerHourUsd", round4(fallbackOnDemand * (1 - SAVINGS_PLAN_3YR_DISCOUNT_PCT / 100.0)));
            return ResponseEntity.ok(response);
        }

        double ri1Yr = fetchRiPrice(instanceType, "1yr", "No Upfront");
        double ri3Yr = fetchRiPrice(instanceType, "3yr", "No Upfront");

        response.put("source", "AWS Price List API (us-east-1 Live)");
        response.put("onDemandPerHourUsd", onDemandPrice);
        response.put("riStandard1yrPerHourUsd", ri1Yr > 0 ? ri1Yr : round4(onDemandPrice * (1 - RI_1YR_STD_DISCOUNT_PCT / 100.0)));
        response.put("riStandard3yrPerHourUsd", ri3Yr > 0 ? ri3Yr : round4(onDemandPrice * (1 - RI_3YR_STD_DISCOUNT_PCT / 100.0)));
        response.put("riConvertible1yrPerHourUsd", round4(onDemandPrice * (1 - RI_CONVERTIBLE_1YR_DISCOUNT_PCT / 100.0)));
        response.put("savingsPlan1yrPerHourUsd", round4(onDemandPrice * (1 - SAVINGS_PLAN_1YR_DISCOUNT_PCT / 100.0)));
        response.put("savingsPlan3yrPerHourUsd", round4(onDemandPrice * (1 - SAVINGS_PLAN_3YR_DISCOUNT_PCT / 100.0)));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/discount")
    public ResponseEntity<DiscountResponse> calculateFinOpsDiscount(@RequestBody DiscountRequest req) {
        List<DiscountOption> options = new ArrayList<>();

        if (req.riStandard1yr)    options.add(new DiscountOption("Standard RI 1-Yr", RI_1YR_STD_DISCOUNT_PCT));
        if (req.riStandard3yr)    options.add(new DiscountOption("Standard RI 3-Yr", RI_3YR_STD_DISCOUNT_PCT));
        if (req.riConvertible1yr) options.add(new DiscountOption("Convertible RI 1-Yr", RI_CONVERTIBLE_1YR_DISCOUNT_PCT));
        if (req.savingsPlan1yr)   options.add(new DiscountOption("Compute Savings Plan 1-Yr", SAVINGS_PLAN_1YR_DISCOUNT_PCT));
        if (req.savingsPlan3yr)   options.add(new DiscountOption("Compute Savings Plan 3-Yr", SAVINGS_PLAN_3YR_DISCOUNT_PCT));

        DiscountOption best = options.stream()
                .max(Comparator.comparingDouble(o -> o.discountPct))
                .orElse(null);

        DiscountResponse resp = new DiscountResponse();
        if (best != null) {
            resp.bestStrategy = best.strategyName;
            resp.maxDiscountPct = best.discountPct;
            resp.calculatedMonthlySavingUsd = round2(req.currentMonthlyContainerCostUsd * (best.discountPct / 100.0));
            resp.netMonthlyContainerCostUsd = round2(req.currentMonthlyContainerCostUsd * (1 - best.discountPct / 100.0));
        } else {
            resp.bestStrategy = null;
            resp.maxDiscountPct = 0.0;
            resp.calculatedMonthlySavingUsd = 0.0;
            resp.netMonthlyContainerCostUsd = req.currentMonthlyContainerCostUsd;
        }

        return ResponseEntity.ok(resp);
    }

    /* ================================================================
       AWS PRICING CLIENT LOGIC
    ================================================================ */

    private double getOnDemandPrice(String instanceType) {
        try (PricingClient pc = PricingClient.builder().region(Region.US_EAST_1).build()) {
            var req = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("instanceType").value(instanceType).build(),
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("productFamily").value("Compute Instance").build(),
                            Filter.builder().type("TERM_MATCH").field("operatingSystem").value("Linux").build(),
                            Filter.builder().type("TERM_MATCH").field("tenancy").value("Shared").build(),
                            Filter.builder().type("TERM_MATCH").field("capacitystatus").value("Used").build(),
                            Filter.builder().type("TERM_MATCH").field("preInstalledSw").value("NA").build()
                    ).maxResults(1).formatVersion("aws_v1").build();

            var result = pc.getProducts(req);
            if (!result.priceList().isEmpty()) {
                return extractUsdPrice(mapper.readTree(result.priceList().get(0)), "OnDemand");
            }
        } catch (Exception e) {
            log.warn("Could not fetch on-demand price for {}: {}", instanceType, e.getMessage());
        }
        // FIX: Changed from string return value "-1" to numeric double constant token -1.0
        return -1.0;
    }

    private double fetchRiPrice(String instanceType, String leaseContractLength, String purchaseOption) {
        try (PricingClient pc = PricingClient.builder().region(Region.US_EAST_1).build()) {
            var req = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("instanceType").value(instanceType).build(),
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("productFamily").value("Compute Instance").build(),
                            Filter.builder().type("TERM_MATCH").field("operatingSystem").value("Linux").build(),
                            Filter.builder().type("TERM_MATCH").field("tenancy").value("Shared").build(),
                            Filter.builder().type("TERM_MATCH").field("offeringClass").value("standard").build(),
                            Filter.builder().type("TERM_MATCH").field("leaseContractLength").value(leaseContractLength).build(),
                            Filter.builder().type("TERM_MATCH").field("purchaseOption").value(purchaseOption).build()
                    ).maxResults(1).formatVersion("aws_v1").build();
            var result = pc.getProducts(req);
            if (!result.priceList().isEmpty()) {
                return extractUsdPrice(mapper.readTree(result.priceList().get(0)), "Reserved");
            }
        } catch (Exception e) {
            log.debug("Could not fetch RI prices for {}: {}", instanceType, e.getMessage());
        }
        return -1.0;
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

    /* ================================================================
       DTO RECORDS
    ================================================================ */
    public static class DiscountRequest {
        @JsonProperty("currentMonthlyContainerCostUsd") public double currentMonthlyContainerCostUsd;
        @JsonProperty("riStandard1yr") public boolean riStandard1yr;
        @JsonProperty("riStandard3yr") public boolean riStandard3yr;
        @JsonProperty("riConvertible1yr") public boolean riConvertible1yr;
        @JsonProperty("savingsPlan1yr") public boolean savingsPlan1yr;
        @JsonProperty("savingsPlan3yr") public boolean savingsPlan3yr;
    }

    public static class DiscountResponse {
        @JsonProperty("bestStrategy") public String bestStrategy;
        @JsonProperty("maxDiscountPct") public double maxDiscountPct;
        @JsonProperty("calculatedMonthlySavingUsd") public double calculatedMonthlySavingUsd;
        @JsonProperty("netMonthlyContainerCostUsd") public double netMonthlyContainerCostUsd;
    }

    private static class DiscountOption {
        final String strategyName;
        final double discountPct;
        DiscountOption(String n, double p) { this.strategyName = n; this.discountPct = p; }
    }
}