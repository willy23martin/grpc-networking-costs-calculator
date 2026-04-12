package com.calculator.application.services.calculators.cost.cloud.aws;

import com.calculator.application.services.calculators.cost.cloud.CloudCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

public abstract class AWSCloudCalculator extends CloudCalculator {

    protected static final Region AWS_PRICING_REGION = Region.US_EAST_1;
    protected static final String AWS_LOCATION = "US East (N. Virginia)";

    protected PricingClient pricing;

    protected AWSCloudCalculator(){
        this.pricing = PricingClient.builder()
                .region(AWS_PRICING_REGION)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    protected double fetchSimplePrice(
            String serviceCode,
            String productFamily,
            double fallback
    ) {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode(serviceCode)
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
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
        } catch (Exception e) {
            return fallback;
        }
    }

}
