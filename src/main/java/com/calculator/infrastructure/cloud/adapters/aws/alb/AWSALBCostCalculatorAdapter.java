package com.calculator.infrastructure.cloud.adapters.aws.alb;

import com.calculator.application.services.calculators.cost.cloud.ports.ALBCostCalculatorPort;
import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
import com.calculator.domain.repository.finops.reliability.FinOpsStrategyReliabilityArchitecturalDecisionRepository;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.*;
import java.util.logging.Logger;

import static com.calculator.application.services.utils.MathUtils.round2;

public class AWSALBCostCalculatorAdapter extends AWSCloudCalculatorAdapter implements ALBCostCalculatorPort {

    private static final Logger log = Logger.getLogger(AWSALBCostCalculatorAdapter.class.getName());
    public static final int HOURS_IN_A_MONTH = 730;

    @Value("${aws.pricing.alb.fixed.charged.per.hour}")
    private double albFixedChargePerHour;

    @Value("${aws.pricing.lcu.fixed.charged.per.hour}")
    private double albLCUFixedChargePerHour;

    private final FinOpsStrategyReliabilityArchitecturalDecisionRepository finOpsStrategyReliabilityArchitecturalDecisionRepository;

    public AWSALBCostCalculatorAdapter(
            PricingClient pricingClient,
            FinOpsStrategyReliabilityArchitecturalDecisionRepository finOpsStrategyReliabilityArchitecturalDecisionRepository
    ) {
        super(pricingClient);
        this.finOpsStrategyReliabilityArchitecturalDecisionRepository = finOpsStrategyReliabilityArchitecturalDecisionRepository;
    }

    @Override
    public Map<String, Object> calculateALBCosts() {
        Map<String, Object> albCostMap = new LinkedHashMap<>();
        try {
            GetProductsResponse getProductsResponse = pricingClient.getProducts(buildRequest());
            log.info("GetProductsResponse" + getProductsResponse);
            JSONLogger.logAsJSON(log, getProductsResponse);
            log.info("ALB raw product count: " + getProductsResponse.priceList().size());

            double fixedPerHour = 0.0;
            final double[] lcuPerHour = {0.0};
            List<Map<String, Object>> lcuTiers = new ArrayList<>();

            for (String productJson : getProductsResponse.priceList()) {
                if (productJson == null || productJson.isBlank()) {
                    log.warning("Skipping null/blank product JSON entry");
                    continue;
                }

                JsonNode root = mapper.readTree(productJson);

                if (root == null || root.isMissingNode()) {
                    log.warning("Skipping unparseable product JSON");
                    continue;
                }

                String usageType = root.path("product")
                        .path("attributes")
                        .path("usagetype")
                        .asText("");

                log.info("Found usagetype: [" + usageType + "]");

                JsonNode onDemandTerms = root.path("terms").path("OnDemand");
                if (!onDemandTerms.fields().hasNext()) {
                    log.warning("No OnDemand terms for usagetype: " + usageType);
                    continue;
                }

                JsonNode termValue = onDemandTerms.fields().next().getValue();
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

                } else if (usageType.contains("DataProcessing-Bytes")) {
                    priceDimensions.fields().forEachRemaining(dimEntry -> {
                        JsonNode priceDimension = dimEntry.getValue();
                        double tierPrice = priceDimension.path("pricePerUnit").path("USD").asDouble(0.0);
                        Map<String, Object> tier = new LinkedHashMap<>();
                        tier.put("description",  priceDimension.path("description").asText());
                        tier.put("beginRange",   priceDimension.path("beginRange").asText());
                        tier.put("endRange",     priceDimension.path("endRange").asText());
                        tier.put("pricePerUnit", tierPrice);
                        tier.put("unit",         priceDimension.path("unit").asText());
                        lcuTiers.add(tier);
                        if (lcuPerHour[0] == 0.0) lcuPerHour[0] = tierPrice;
                    });
                    log.info("Resolved lcuPerHour: " + lcuPerHour[0]);
                }
            }

            double resolvedFixed = fixedPerHour > 0 ? fixedPerHour : albFixedChargePerHour;
            double resolvedLcu   = lcuPerHour[0] > 0 ? lcuPerHour[0] : albLCUFixedChargePerHour;

            albCostMap.put("fixedPerHourUsd",  resolvedFixed);
            albCostMap.put("lcuPerHourUsd",    resolvedLcu);
            albCostMap.put("fixedPerMonthUsd", round2(resolvedFixed * HOURS_IN_A_MONTH));
            albCostMap.put("lcuPerMonthBase",  round2(resolvedLcu   * HOURS_IN_A_MONTH));
            albCostMap.put("lcuPricingTiers",  lcuTiers.isEmpty()
                    ? List.of(Map.of("note", "no tiers returned"))
                    : lcuTiers);
            albCostMap.put("finopsNotes",      buildFinopsNotes(resolvedFixed, resolvedLcu)); // TODO - to be shown in the Unit Economics notes
            albCostMap.put("source",           fixedPerHour > 0 ? "AWS Pricing API" : "fallback");

        } catch (Exception e) {
            e.printStackTrace();
            log.warning("ALB pricing fetch failed: " + e.getMessage());
            setFallbackValues(albCostMap);
        }
        return albCostMap;
    }

    private void setFallbackValues(Map<String, Object> albCostMap) {
        albCostMap.put("fixedPerHourUsd",   albFixedChargePerHour);
        albCostMap.put("lcuPerHourUsd",     albLCUFixedChargePerHour);
        albCostMap.put("fixedPerMonthUsd",  5.84);
        albCostMap.put("lcuPerMonthBase",   5.84);
        albCostMap.put("source",            "fallback");
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
                                .value("Load Balancer")
                                .build()
                )
                .formatVersion("aws_v1")
                .maxResults(100) // Increase slightly to ensure both Usage and LCU types return in the same page
                .build();
    }

    private List<String> buildFinopsNotes(double fixedPerHour, double lcuPerHour) {
        List<String> notes = new ArrayList<>();
        notes.add(String.format("Fixed: $%.4f/hr (~$%.2f/mo) — unavoidable while ALB exists", fixedPerHour, fixedPerHour * HOURS_IN_A_MONTH));
        notes.add(String.format("LCU: $%.4f/LCU/hr — reduce via HTTP keep-alive, fewer listener rules, target consolidation", lcuPerHour));

        var strategy = finOpsStrategyReliabilityArchitecturalDecisionRepository.getFinOpsStrategyForAWSApplicationLoadBalancer();
        if (strategy != null && strategy.getCostFactor() != null && strategy.getCostFactor().getCostFactorNotes() != null) {
            notes.add(strategy.getCostFactor().getCostFactorNotes());
        }

        return notes;
    }

    @PostConstruct
    private void init(){
        calculateALBCosts();
    }

}