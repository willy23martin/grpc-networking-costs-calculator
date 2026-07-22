package com.calculator.infrastructure.cloud.adapters.aws.compute;

import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
import com.calculator.application.services.calculators.cost.cloud.ports.CloudComputeCostCalculatorPort;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import static com.calculator.application.services.calculators.CostEfficiencyCalculator.HOURS_PER_MONTH;

public class AWSComputeCostCalculatorAdapter extends AWSCloudCalculatorAdapter implements CloudComputeCostCalculatorPort {

    private static final Logger log = Logger.getLogger(AWSComputeCostCalculatorAdapter.class.getName());

    public static final int GIGA_BITS = 1_000_000_000;
    public static final int BITS_PER_BYTE = 8;

    //Regex extractor safely translating string values ("12.5 Gbps", "Up to 5 Gbps") into structured doubles.
    public static final String REGEX = "(\\d+(?:\\.\\d+)?)";

    public AWSComputeCostCalculatorAdapter(PricingClient pricingClient) {
        super(pricingClient);
    }

    private static class Ec2InstanceSpecs {
        public final double price;
        public final double networkGbps;

        public Ec2InstanceSpecs(double price, double networkGbps) {
            this.price = price;
            this.networkGbps = networkGbps;
        }
    }

    @PostConstruct
    @Override
    public List<Map<String, Object>> calculatePriceByComputeInstance() {
        List<String> ec2ComputeFamilies = List.of(
                "t3.micro", "t3.small", "t3.medium", "t3.large",
                "m6i.large", "m6i.xlarge", "m6i.2xlarge", "m6i.4xlarge", "m6i.8xlarge",
                "c6i.large", "c6i.xlarge", "c6i.2xlarge", "c6i.4xlarge", "c6i.8xlarge",
                "r6i.large", "r6i.xlarge", "r6i.2xlarge", "r6i.4xlarge"
        );

        List<Map<String, Object>> priceByComputeInstance = new ArrayList<>();

        for (String instanceType : ec2ComputeFamilies) {
            try {
                // Unified single network call fetching price & network bandwidth concurrently
                Ec2InstanceSpecs specs = fetchEc2InstanceSpecs(instanceType);

                double gigaBitsPerSecond = specs.networkGbps;
                long bytesPerSecond = (long) (gigaBitsPerSecond * GIGA_BITS / BITS_PER_BYTE);

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("instanceType", instanceType);
                entry.put("networkGbps", gigaBitsPerSecond);
                entry.put("networkBytesPerSec", bytesPerSecond);
                entry.put("pricePerHourUsd", specs.price);
                entry.put("pricePerMonthUsd", Math.round(specs.price * HOURS_PER_MONTH));

                priceByComputeInstance.add(entry);
            } catch (Exception e) {
                log.warning("Failed to calculate specs or fetch price for " + instanceType + ": " + e.getMessage());
            }
        }
        return priceByComputeInstance;
    }

    private Ec2InstanceSpecs fetchEc2InstanceSpecs(String instanceType) {
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
            log.info("GetProductsResponse for " + instanceType + ": " + resp);
            JSONLogger.logAsJSON(log, resp);

            if (resp.priceList().isEmpty()) {
                return new Ec2InstanceSpecs(0.0, getLocalNetworkFallback(instanceType));
            }

            JsonNode root = mapper.readTree(resp.priceList().getFirst());

            double networkGbps = 1.0;
            JsonNode networkAttr = root.path("product").path("attributes").path("networkPerformance");
            if (!networkAttr.isMissingNode()) {
                networkGbps = parseNetworkGbpsString(networkAttr.asText(), instanceType);
            }

            double price = 0.0;
            JsonNode onDemandNode = root.path("terms").path("OnDemand");

            if (!onDemandNode.isMissingNode() && onDemandNode.fieldNames().hasNext()) {
                String termKey = onDemandNode.fieldNames().next();
                JsonNode priceDimensionsNode = onDemandNode.path(termKey).path("priceDimensions");

                Iterator<String> fieldNames = priceDimensionsNode.fieldNames();
                while (fieldNames.hasNext()) {
                    String fieldName = fieldNames.next();
                    JsonNode dimensionValue = priceDimensionsNode.path(fieldName);
                    JsonNode usdNode = dimensionValue.path("pricePerUnit").path("USD");

                    if (!usdNode.isMissingNode() && usdNode.asDouble() > 0.0) {
                        price = usdNode.asDouble();
                        break; // Found the active hourly compute charge
                    }
                }
            }

            return new Ec2InstanceSpecs(price, networkGbps);

        } catch (Exception e) {
            log.severe("AWS Pricing API call failed for " + instanceType + ". Shifting to architecture defaults. Msg: " + e.getMessage());

            // Revert back to baseline hardcoded structural values if AWS drops connection
            Map<String, Double> priceFallback = Map.of(
                    "t3.micro", 0.0104, "t3.small", 0.0208, "t3.medium", 0.0416,
                    "m6i.large", 0.096, "m6i.xlarge", 0.192, "c6i.large", 0.085,
                    "r6i.large", 0.126
            );
            double fallbackPrice = priceFallback.getOrDefault(instanceType, 0.10);
            double fallbackNetwork = getLocalNetworkFallback(instanceType);

            return new Ec2InstanceSpecs(fallbackPrice, fallbackNetwork);
        }
    }

    private double parseNetworkGbpsString(String networkText, String instanceType) {
        if (networkText == null || networkText.isBlank()) {
            return getLocalNetworkFallback(instanceType);
        }
        try {
            Matcher matcher = Pattern.compile(REGEX).matcher(networkText);
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1));
            }
        } catch (Exception e) {
            log.warning("Could not execute regex on network performance text: '" + networkText + "'. Shifting to fallback.");
        }
        return getLocalNetworkFallback(instanceType);
    }

    private double getLocalNetworkFallback(String instanceType) {
        if (instanceType == null) return 1.0;
        if (instanceType.startsWith("t3.")) return 0.5;
        if (instanceType.contains("4xlarge") || instanceType.contains("8xlarge")) return 25.0;
        return 12.5; // Default reference baseline handling m6i, c6i, and r6i family standards
    }
}