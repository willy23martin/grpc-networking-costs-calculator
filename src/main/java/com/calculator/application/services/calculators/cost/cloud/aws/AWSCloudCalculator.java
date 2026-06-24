package com.calculator.application.services.calculators.cost.cloud.aws;

import com.calculator.application.services.calculators.cost.cloud.CloudCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.logging.Logger;

public abstract class AWSCloudCalculator extends CloudCalculator {

    protected static final Region AWS_PRICING_REGION = Region.US_EAST_1;
    protected static final String AWS_LOCATION = "US East (N. Virginia)";

    private static final Logger log = Logger.getLogger(AWSCloudCalculator.class.getName());

    @Autowired
    protected PricingClient pricingClient;

    protected double fetchSimplePrice(String serviceCode, String productFamily, double fallback) {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode(serviceCode)
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("productFamily").value(productFamily).build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();

            GetProductsResponse resp = pricingClient.getProducts(req); // <-- mock throws here

            if (resp.priceList().isEmpty()) {
                log.info("GetProductsResponse is empty, using fallback for: " + serviceCode + "/" + productFamily);
                return fallback;
            }

            String productJson = resp.priceList().get(0);
            if (productJson == null || productJson.isBlank()) return fallback;

            JsonNode root = mapper.readTree(productJson);
            if (root == null || root.isMissingNode()) return fallback;

            double price = root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(fallback);

            return price > 0 ? price : fallback;

        } catch (Exception e) {  // catches RuntimeException, IOException, NoSuchElementException, everything
            log.warning("fetchSimplePrice failed for " + serviceCode + "/" + productFamily + ": " + e.getMessage());
            return fallback;  // always returns fallback, never propagates
        }
    }

}
