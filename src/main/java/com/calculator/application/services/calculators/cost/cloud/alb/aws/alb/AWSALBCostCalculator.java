package com.calculator.application.services.calculators.cost.cloud.alb.aws.alb;

import com.calculator.application.services.calculators.cost.cloud.alb.ALBCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public class AWSALBCostCalculator extends AWSCloudCalculator implements ALBCostCalculator {

    private static final Logger log = Logger.getLogger(AWSALBCostCalculator.class.getName());

    @Override
    public Map<String, Object> calculateALBCosts() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            // ALB fixed charge: $0.008/hr per ALB
            // LCU charge: $0.008/LCU-hr  (1 LCU = 25 new connections/s OR 3000 active connections
            //              OR 1 GB/hr processed OR 1000 rule evaluations/s)
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AWSElasticLoadBalancing")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
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

}
