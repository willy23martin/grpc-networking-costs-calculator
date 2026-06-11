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

    private static final double ALB_FIXED_CHARGE_PER_HOUR = 0.008;

    private static final double LCU_FIXED_CHARGE_PER_HOUR = 0.008; // Because: (1 LCU = 25 new connections/s OR 3000 active connections OR 1 GB/hr processed OR 1000 rule evaluations/s)

    @Override
    public Map<String, Object> calculateALBCosts() {

        Map<String, Object> albCostMap = new LinkedHashMap<>();
        try {

            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AWSElasticLoadBalancing")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("group").value("ALB:LoadBalancer").build()
                    )
                    .formatVersion("aws_v1").maxResults(10).build();
            GetProductsResponse resp = pricing.getProducts(req);
            System.out.println("GetProductsResponse: " +  resp);
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
            albCostMap.put("fixedPerHourUsd",   fixedPerHour > 0 ? fixedPerHour : ALB_FIXED_CHARGE_PER_HOUR);
            albCostMap.put("lcuPerHourUsd",     lcuPerHour   > 0 ? lcuPerHour   : LCU_FIXED_CHARGE_PER_HOUR);
            albCostMap.put("fixedPerMonthUsd",  Math.round((fixedPerHour > 0 ? fixedPerHour : ALB_FIXED_CHARGE_PER_HOUR) * 730 * 100) / 100.0);
            albCostMap.put("lcuPerMonthBase",   Math.round((lcuPerHour   > 0 ? lcuPerHour   : LCU_FIXED_CHARGE_PER_HOUR) * 730 * 100) / 100.0);
            albCostMap.put("source", "AWS Pricing API");
        } catch (Exception e) {
            log.warning("ALB pricing fetch failed: " + e.getMessage());
            albCostMap.put("fixedPerHourUsd",  ALB_FIXED_CHARGE_PER_HOUR);
            albCostMap.put("lcuPerHourUsd",    LCU_FIXED_CHARGE_PER_HOUR);
            albCostMap.put("fixedPerMonthUsd", 5.84);
            albCostMap.put("lcuPerMonthBase",  5.84);
            albCostMap.put("source", "fallback");
        }
        return albCostMap;
    }

}
