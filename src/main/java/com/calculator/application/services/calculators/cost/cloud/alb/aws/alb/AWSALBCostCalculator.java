package com.calculator.application.services.calculators.cost.cloud.alb.aws.alb;

import com.calculator.application.services.calculators.cost.cloud.alb.ALBCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.*;
import java.util.logging.Logger;

public class AWSALBCostCalculator extends AWSCloudCalculator implements ALBCostCalculator {

    private static final Logger log = Logger.getLogger(AWSALBCostCalculator.class.getName());

    private static final double ALB_FIXED_CHARGE_PER_HOUR = 0.008;
    private static final double LCU_FIXED_CHARGE_PER_HOUR = 0.008;

    @PostConstruct
    @Override
    public Map<String, Object> calculateALBCosts() {
        Map<String, Object> albCostMap = new LinkedHashMap<>();
        try {
            GetProductsResponse resp = pricingClient.getProducts(buildRequest());
            log.info("GetProductsResponse" + resp);
            JSONLogger.logAsJSON(log, resp);
            log.info("ALB raw product count: " + resp.priceList().size());

            double fixedPerHour = 0.0;
            final double[] lcuPerHour = {0.0};
            List<Map<String, Object>> lcuTiers = new ArrayList<>();

            for (String productJson : resp.priceList()) {
                // FIX: guard against null/blank JSON strings from the API or mocks
                if (productJson == null || productJson.isBlank()) {
                    log.warning("Skipping null/blank product JSON entry");
                    continue;
                }

                JsonNode root = mapper.readTree(productJson);

                // FIX: guard against readTree returning null
                if (root == null || root.isMissingNode()) {
                    log.warning("Skipping unparseable product JSON");
                    continue;
                }

                String usageType = root.path("product")
                        .path("attributes")
                        .path("usagetype")
                        .asText("");

                log.info("Found usagetype: [" + usageType + "]");

                JsonNode onDemand = root.path("terms").path("OnDemand");
                if (!onDemand.fields().hasNext()) {
                    log.warning("No OnDemand terms for usagetype: " + usageType);
                    continue;
                }

                JsonNode termValue = onDemand.fields().next().getValue();
                JsonNode priceDimensions = termValue.path("priceDimensions");
                if (!priceDimensions.fields().hasNext()) {
                    log.warning("No priceDimensions for usagetype: " + usageType);
                    continue;
                }

                if (usageType.contains("LoadBalancerUsage") && fixedPerHour == 0.0) {
                    fixedPerHour = priceDimensions.fields().next()
                            .getValue()
                            .path("pricePerUnit")
                            .path("USD")
                            .asDouble(0.0);
                    log.info("Resolved fixedPerHour: " + fixedPerHour);

                } else if (usageType.contains("LCUUsage")) {
                    priceDimensions.fields().forEachRemaining(dimEntry -> {
                        JsonNode dim = dimEntry.getValue();
                        double tierPrice = dim.path("pricePerUnit").path("USD").asDouble(0.0);
                        Map<String, Object> tier = new LinkedHashMap<>();
                        tier.put("description",  dim.path("description").asText());
                        tier.put("beginRange",   dim.path("beginRange").asText());
                        tier.put("endRange",     dim.path("endRange").asText());
                        tier.put("pricePerUnit", tierPrice);
                        tier.put("unit",         dim.path("unit").asText());
                        lcuTiers.add(tier);
                        if (lcuPerHour[0] == 0.0) lcuPerHour[0] = tierPrice;
                    });
                    log.info("Resolved lcuPerHour: " + lcuPerHour[0]);
                }
            }

            double resolvedFixed = fixedPerHour > 0 ? fixedPerHour : ALB_FIXED_CHARGE_PER_HOUR;
            double resolvedLcu   = lcuPerHour[0] > 0 ? lcuPerHour[0] : LCU_FIXED_CHARGE_PER_HOUR;

            albCostMap.put("fixedPerHourUsd",  resolvedFixed);
            albCostMap.put("lcuPerHourUsd",    resolvedLcu);
            albCostMap.put("fixedPerMonthUsd", round2(resolvedFixed * 730));
            albCostMap.put("lcuPerMonthBase",  round2(resolvedLcu   * 730));
            albCostMap.put("lcuPricingTiers",  lcuTiers.isEmpty()
                    ? List.of(Map.of("note", "no tiers returned"))
                    : lcuTiers);
            albCostMap.put("finopsNotes",      buildFinopsNotes(resolvedFixed, resolvedLcu));
            albCostMap.put("source",           fixedPerHour > 0 ? "AWS Pricing API" : "fallback");

        } catch (Exception e) {
            log.warning("ALB pricing fetch failed: " + e.getMessage());
            albCostMap.put("fixedPerHourUsd",   ALB_FIXED_CHARGE_PER_HOUR);
            albCostMap.put("lcuPerHourUsd",     LCU_FIXED_CHARGE_PER_HOUR);
            albCostMap.put("fixedPerMonthUsd",  5.84);
            albCostMap.put("lcuPerMonthBase",   5.84);
            albCostMap.put("source",            "fallback");
        }
        return albCostMap;
    }

    private GetProductsRequest buildRequest() {
        return GetProductsRequest.builder()
                .serviceCode("AmazonEC2") // FIX: AWS catalog groups ELB under AmazonEC2
                .filters(
                        Filter.builder()
                                .type(FilterType.TERM_MATCH)
                                .field("location")
                                .value(AWS_LOCATION)
                                .build(),
                        Filter.builder()
                                .type(FilterType.TERM_MATCH)
                                .field("productFamily")
                                .value("Load Balancer") // FIX: Must be "Load Balancer"
                                .build()
                )
                .formatVersion("aws_v1")
                .maxResults(100) // Increase slightly to ensure both Usage and LCU types return in the same page
                .build();
    }

    private List<String> buildFinopsNotes(double fixedPerHour, double lcuPerHour) {
        return List.of(
                String.format("Fixed: $%.4f/hr (~$%.2f/mo) — unavoidable while ALB exists", fixedPerHour, fixedPerHour * 730),
                String.format("LCU: $%.4f/LCU/hr — reduce via HTTP keep-alive, fewer listener rules, target consolidation", lcuPerHour),
                "ALB has NO Reserved Instances or Savings Plans — only usage reduction cuts cost",
                "Consider NLB for pure TCP/UDP: NLCU pricing is often cheaper than ALB LCU at scale",
                "Use Cost Explorer filter: UsageType = 'LoadBalancerUsage' and 'LCUUsage' to track separately"
        );
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

}