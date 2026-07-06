package com.calculator.application.services.calculators.cost.cloud.compute.aws.eks;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.Iterator;
import java.util.logging.Logger;

@Service
public class EKSComputeCostCalculator extends AWSCloudCalculator {

    private static final Logger log = Logger.getLogger(EKSComputeCostCalculator.class.getName());
    private static final double FALLBACK_EKS_HOURLY = 0.10; // $73.0/mo baseline

    @PostConstruct
    public double fetchEksControlPlaneHourlyCost() {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonEKS")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("productFamily").value("Compute").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("operation").value("CreateOperation").build()
                    )
                    .formatVersion("aws_v1")
                    .maxResults(1)
                    .build();

            GetProductsResponse resp = pricingClient.getProducts(req);
            log.info("GetProductsResponse: " + resp);
            JSONLogger.logAsJSON(log, resp);

            if (resp.priceList().isEmpty()) {
                return FALLBACK_EKS_HOURLY;
            }

            JsonNode root = mapper.readTree(resp.priceList().getFirst());
            JsonNode onDemandNode = root.path("terms").path("OnDemand");

            if (!onDemandNode.isMissingNode() && onDemandNode.fieldNames().hasNext()) {
                String termKey = onDemandNode.fieldNames().next();
                JsonNode priceDimensionsNode = onDemandNode.path(termKey).path("priceDimensions");

                Iterator<String> fieldNames = priceDimensionsNode.fieldNames();
                if (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode usdNode = priceDimensionsNode.path(fieldName).path("pricePerUnit").path("USD");
                    if (!usdNode.isMissingNode() && usdNode.asDouble() > 0.0) {
                        return usdNode.asDouble();
                    }
                }
            }
        } catch (Exception e) {
            log.severe("AWS Pricing API call failed for AmazonEKS. Using fallback default. Msg: " + e.getMessage());
        }
        return FALLBACK_EKS_HOURLY;
    }
}