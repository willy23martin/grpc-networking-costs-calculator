package com.calculator.application.services.calculators.cost.cloud.compute.aws.ec2;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.compute.CloudComputeCostCalculator;
import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class EC2ComputeCostCalculator extends AWSCloudCalculator implements CloudComputeCostCalculator {

    private static final Logger log = Logger.getLogger(EC2ComputeCostCalculator.class.getName());

    public static final int GIGA_BITS = 1_000_000_000;
    public static final int BITS_PER_BYTE = 8;
    public static final int HOURS_PER_MONTH = 740;

    @Override
    public List<Map<String, Object>> calculatePriceByComputeInstance() {
        List<String> ec2ComputeFamilies = List.of(
                "t3.micro","t3.small","t3.medium","t3.large",
                "m6i.large","m6i.xlarge","m6i.2xlarge","m6i.4xlarge","m6i.8xlarge",
                "c6i.large","c6i.xlarge","c6i.2xlarge","c6i.4xlarge","c6i.8xlarge",
                "r6i.large","r6i.xlarge","r6i.2xlarge","r6i.4xlarge"
        );

        Map<String,Double> networkGbpsPerEc2InstanceMap = Map.ofEntries(
                Map.entry("t3.micro",   0.5),
                Map.entry("t3.small",   0.5),
                Map.entry("t3.medium",  0.5),
                Map.entry("t3.large",   0.5),
                Map.entry("m6i.large",  12.5),
                Map.entry("m6i.xlarge",  12.5),
                Map.entry("m6i.2xlarge",12.5),
                Map.entry("m6i.4xlarge",  25.0),
                Map.entry("m6i.8xlarge",25.0),
                Map.entry("c6i.large",  12.5),
                Map.entry("c6i.xlarge",  12.5),
                Map.entry("c6i.2xlarge",12.5),
                Map.entry("c6i.4xlarge",  25.0),
                Map.entry("c6i.8xlarge",25.0),
                Map.entry("r6i.large",  12.5),
                Map.entry("r6i.xlarge",  12.5),
                Map.entry("r6i.2xlarge",12.5),
                Map.entry("r6i.4xlarge",  25.0)
        );

        List<Map<String, Object>> priceByComputeInstance = new ArrayList<>();
        for (String instanceType : ec2ComputeFamilies) {
            try {
                double price  = fetchEc2OnDemandPrice(instanceType);
                double gigaBitsPerSecond = networkGbpsPerEc2InstanceMap.getOrDefault(instanceType, 1.0);
                long bytesPerSecond= (long)(gigaBitsPerSecond * GIGA_BITS / BITS_PER_BYTE);

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("instanceType", instanceType);
                entry.put("networkGbps",  gigaBitsPerSecond);
                entry.put("networkBytesPerSec", bytesPerSecond);
                entry.put("pricePerHourUsd", price);
                entry.put("pricePerMonthUsd", Math.round(price * HOURS_PER_MONTH));
                priceByComputeInstance.add(entry);
            } catch (Exception e) {
                log.warning("Failed to fetch price for " + instanceType + ": " + e.getMessage());
            }
        }
        return priceByComputeInstance;
    }

    private double fetchEc2OnDemandPrice(String instanceType) {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value(instanceType).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("operatingSystem").value("Linux").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("tenancy").value("Shared").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("preInstalledSw").value("NA").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("capacitystatus").value("Used").build()
                    )
                    .formatVersion("aws_v1")
                    .maxResults(1)
                    .build();
            GetProductsResponse resp = pricingClient.getProducts(req);
            if (resp.priceList().isEmpty()) return 0.0;
            JsonNode root = mapper.readTree(resp.priceList().get(0));
            return root.path("terms").path("OnDemand").fields().next()
                    .getValue().path("priceDimensions").fields().next()
                    .getValue().path("pricePerUnit").path("USD").asDouble(0.0);
        } catch (Exception e) {
            Map<String,Double> fallback = Map.of(
                    "t3.micro",0.0104, "t3.small",0.0208, "t3.medium",0.0416,
                    "m6i.large",0.096, "m6i.xlarge",0.192, "c6i.large",0.085,
                    "r6i.large",0.126
            );
            return fallback.getOrDefault(instanceType, 0.10);
        }
    }
}
