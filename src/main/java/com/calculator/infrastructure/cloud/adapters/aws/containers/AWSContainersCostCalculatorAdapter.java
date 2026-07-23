package com.calculator.infrastructure.cloud.adapters.aws.containers;

import com.calculator.application.services.calculators.cost.cloud.ports.ContainerizedCostCalculatorPort;
import com.calculator.domain.dto.responses.containers.ContainerPricingResponse;
import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.*;
import java.util.logging.Logger;

import static com.calculator.application.services.calculators.CostEfficiencyCalculator.HOURS_PER_MONTH;

public class AWSContainersCostCalculatorAdapter extends AWSCloudCalculatorAdapter implements ContainerizedCostCalculatorPort {

    private static final Logger log = Logger.getLogger(AWSContainersCostCalculatorAdapter.class.getName());

    public static final double FALLBACK_EKS_HOURLY = 0.10; // Because of $73.0/mo baseline

    // EC2 instance approximate on-demand prices (us-east-1, Linux, on-demand, Q1-2025)
    /**
     * * **Primary Reference URL:** [Amazon EC2 On-Demand Pricing - AWS](https://aws.amazon.com/ec2/pricing/on-demand/)
     * * **Instance Family Overviews & Specifications:**
     *   * [Amazon EC2 Instance Types (M6i)](https://aws.amazon.com/ec2/instance-types/m6i/)
     *   * [Amazon EC2 Pricing Overview](https://aws.amazon.com/ec2/pricing/)
     */
    public static final Map<String, Double> EC2_FALLBACK_PRICES = new HashMap<>();

    static {
        EC2_FALLBACK_PRICES.put("t3.medium", 0.0416);
        EC2_FALLBACK_PRICES.put("t3.large", 0.0832);
        EC2_FALLBACK_PRICES.put("m6i.large", 0.096);
        EC2_FALLBACK_PRICES.put("m6i.xlarge", 0.192);
        EC2_FALLBACK_PRICES.put("m6i.2xlarge", 0.384);
        EC2_FALLBACK_PRICES.put("c6i.large", 0.085);
        EC2_FALLBACK_PRICES.put("c6i.xlarge", 0.170);
        EC2_FALLBACK_PRICES.put("m6g.large", 0.077);
        EC2_FALLBACK_PRICES.put("m6g.xlarge", 0.154);
        EC2_FALLBACK_PRICES.put("m6g.2xlarge", 0.308);
        EC2_FALLBACK_PRICES.put("c6g.large", 0.068);
        EC2_FALLBACK_PRICES.put("c6g.xlarge", 0.136);
        EC2_FALLBACK_PRICES.put("r6g.large", 0.1008);
    }

    private static final String NOTE_SOURCE = "AWS Pricing API (us-east-1) — live Q1-2025";

    // Fargate compute: per vCPU-hour and per GB-hour
    public static final double FARGATE_VCPU_PER_HOUR = 0.04048;
    public static final double FARGATE_GB_PER_HOUR = 0.004445;
    public static final double FARGATE_SPOT_VCPU_PER_HOUR = 0.01254688; // ~69% off
    public static final double FARGATE_SPOT_GB_PER_HOUR = 0.00137248;

    // EBS gp3 default: $0.08/GB-month; provisioned IOPS/throughput extra
    public static final double EBS_GP3_PER_GB_MONTH = 0.08;
    public static final double EBS_GP3_IOPS_PER_IOPS_MONTH = 0.005;  // beyond 3000 free
    public static final double EBS_GP3_THROUGHPUT_PER_MBPS = 0.040;  // beyond 125 MiB/s free

    // ALB (same as AWSALBCostCalculator)
    public static final double ALB_FIXED_PER_HOUR = 0.025;
    public static final double ALB_LCU_PER_HOUR = 0.008;
    public static final double ALB_FIXED_PER_MONTH = 0.025 * HOURS_PER_MONTH; // $18.25/mo

    // ECR image storage
    public static final double ECR_STORAGE_PER_GB_MONTH = 0.10;
    public static final double ECR_DATA_TRANSFER_PER_GB = 0.09;  // same as S3 egress

    // AWS WAF
    public static final double WAF_WEB_ACL_PER_MONTH = 5.00;
    public static final double WAF_RULE_PER_MONTH = 1.00;
    public static final double WAF_PER_1M_REQUESTS = 0.60;

    public AWSContainersCostCalculatorAdapter(PricingClient pricingClient) {
        super(pricingClient);
    }

    @Override
    public ContainerPricingResponse getContainerPricingResponse() {
        ContainerPricingResponse containerPricingResponse = new ContainerPricingResponse();

        containerPricingResponse.eksControlPlanePerMonth = fetchContainersHourlyCost() * HOURS_PER_MONTH;
        containerPricingResponse.eksControlPlanePerHour = fetchContainersHourlyCost();
        containerPricingResponse.fargateVcpuPerHour = FARGATE_VCPU_PER_HOUR;
        containerPricingResponse.fargateGbPerHour = FARGATE_GB_PER_HOUR;
        containerPricingResponse.fargateSpotVcpuPerHour = FARGATE_SPOT_VCPU_PER_HOUR;
        containerPricingResponse.fargateSpotGbPerHour = FARGATE_SPOT_GB_PER_HOUR;
        containerPricingResponse.ebsGp3PerGbMonth = EBS_GP3_PER_GB_MONTH;
        containerPricingResponse.ebsGp3IopsPerIopsMonth = EBS_GP3_IOPS_PER_IOPS_MONTH;
        containerPricingResponse.ebsGp3ThroughputPerMbpsMonth = EBS_GP3_THROUGHPUT_PER_MBPS;
        containerPricingResponse.albFixedPerMonth = ALB_FIXED_PER_MONTH;
        containerPricingResponse.albFixedPerHour = ALB_FIXED_PER_HOUR;
        containerPricingResponse.albLcuPerHour = ALB_LCU_PER_HOUR;
        containerPricingResponse.ecrStoragePerGbMonth = ECR_STORAGE_PER_GB_MONTH;
        containerPricingResponse.ecrDataTransferPerGb = ECR_DATA_TRANSFER_PER_GB;
        containerPricingResponse.wafWebAclPerMonth = WAF_WEB_ACL_PER_MONTH;
        containerPricingResponse.wafRulePerMonth = WAF_RULE_PER_MONTH;
        containerPricingResponse.wafPer1MRequests = WAF_PER_1M_REQUESTS;

        containerPricingResponse.source = NOTE_SOURCE;
        containerPricingResponse.note   = "EKS control plane, Fargate (incl. Spot), EBS gp3 (storage/IOPS/throughput), ALB, ECR, WAF pricing for us-east-1. "
                + "EC2 on-demand prices for common node group instance types. "
                + "Spot and RI discounts applied client-side.";

        // Populate EC2 prices: try live API, fall back to hardcoded
        containerPricingResponse.ec2OnDemandPrices.putAll(EC2_FALLBACK_PRICES); // start with fallbacks
        try {
            enrichEc2Prices(containerPricingResponse.ec2OnDemandPrices);
            containerPricingResponse.source = "AWS Pricing API (live) + fallback for unavailable types";
        } catch (Exception e) {
            log.warning("Could not fetch EC2 prices from Pricing API:" + e.getMessage());
            containerPricingResponse.source = "Hardcoded fallback (Q1-2025) — AWS Pricing API unavailable";
        }

        // Try to get live Fargate prices (override fallbacks if successful)
        try {
            double[] fargatePrices = fetchFargatePrices();
            containerPricingResponse.fargateVcpuPerHour = fargatePrices[0];
            containerPricingResponse.fargateGbPerHour   = fargatePrices[1];
        } catch (Exception e) {
            log.warning("Could not fetch Fargate prices: " + e.getMessage());
        }
        return containerPricingResponse;
    }

    @Override
    public double fetchContainersHourlyCost() {
        try {
            GetProductsRequest productsRequest = GetProductsRequest.builder()
                    .serviceCode("AmazonEKS")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("productFamily").value("Compute").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("operation").value("CreateOperation").build()
                    )
                    .formatVersion("aws_v1")
                    .maxResults(1)
                    .build();

            GetProductsResponse resp = pricingClient.getProducts(productsRequest);
            log.info("fetchContainersHourlyCost: " + resp);
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

    private void enrichEc2Prices(Map<String, Double> prices) {
        List<String> instanceTypes = new ArrayList<>(prices.keySet());
        for (String instanceType : instanceTypes) {
            try {
                GetProductsRequest productsRequest = GetProductsRequest.builder()
                        .serviceCode("AmazonEC2")
                        .filters(
                                Filter.builder().type("TERM_MATCH").field("instanceType").value(instanceType).build(),
                                Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                                Filter.builder().type("TERM_MATCH").field("operatingSystem").value("Linux").build(),
                                Filter.builder().type("TERM_MATCH").field("tenancy").value("Shared").build(),
                                Filter.builder().type("TERM_MATCH").field("preInstalledSw").value("NA").build(),
                                Filter.builder().type("TERM_MATCH").field("capacitystatus").value("Used").build()
                        )
                        .maxResults(1)
                        .formatVersion("aws_v1")
                        .build();

                GetProductsResponse result = pricingClient.getProducts(productsRequest);
                log.info("enrichEc2Prices: " + result);
                JSONLogger.logAsJSON(log, result);

                if (!result.priceList().isEmpty()) {
                    String priceJson = result.priceList().getFirst();
                    JsonNode root = mapper.readTree(priceJson);
                    double price = extractOnDemandPriceFromJson(root);
                    if (price > 0) prices.put(instanceType, price);
                }
            } catch (Exception e) {
                log.severe("Could not fetch price for " + instanceType + ": " + e.getMessage());
            }
        }
    }

    private double[] fetchFargatePrices() {
        double vcpuPrice = FARGATE_VCPU_PER_HOUR;
        double gbPrice   = FARGATE_GB_PER_HOUR;
        try {
            // Fargate vCPU
            GetProductsRequest vcpuReq = GetProductsRequest.builder()
                    .serviceCode("AmazonECS")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("usagetype").value("USE1-Fargate-vCPU-Hours:perCPU").build()
                    )
                    .maxResults(1)
                    .formatVersion("aws_v1")
                    .build();

            GetProductsResponse vcpuResult = pricingClient.getProducts(vcpuReq);
            log.info("GetProductsResponse" + vcpuResult);
            JSONLogger.logAsJSON(log, vcpuResult);

            if (!vcpuResult.priceList().isEmpty()) {
                double price = extractOnDemandPriceFromJson(mapper.readTree(vcpuResult.priceList().getFirst()));
                if (price > 0) vcpuPrice = price;
            }

            // Fargate GB
            GetProductsRequest gbReq = GetProductsRequest.builder()
                    .serviceCode("AmazonECS")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("usagetype").value("USE1-Fargate-GB-Hours").build()
                    )
                    .maxResults(1)
                    .formatVersion("aws_v1")
                    .build();

            var gbResult = pricingClient.getProducts(gbReq);
            log.info("fetchFargatePrices: " + gbResult);
            JSONLogger.logAsJSON(log, gbResult);

            if (!gbResult.priceList().isEmpty()) {
                double price = extractOnDemandPriceFromJson(mapper.readTree(gbResult.priceList().getFirst()));
                if (price > 0) gbPrice = price;
            }

            return new double[]{vcpuPrice, gbPrice};
        } catch (Exception e) {
          log.severe("Error fetchFargatePrices: " + e);
          log.info("Returning fallbackValues:" + FARGATE_VCPU_PER_HOUR + " - " + FARGATE_GB_PER_HOUR);
          return new double[]{FARGATE_VCPU_PER_HOUR, FARGATE_GB_PER_HOUR};
        }
    }

    private double extractOnDemandPriceFromJson(JsonNode root) {
        try {
            JsonNode terms = root.path("terms").path("OnDemand");
            Iterator<JsonNode> termIter = terms.elements();
            if (!termIter.hasNext()) return -1;
            JsonNode term = termIter.next();
            JsonNode priceDimensions = term.path("priceDimensions");
            Iterator<JsonNode> dimensionsIterator = priceDimensions.elements();
            if (!dimensionsIterator.hasNext()) return -1;
            JsonNode dimension = dimensionsIterator.next();
            String usdPrice = dimension.path("pricePerUnit").path("USD").asText("0");
            return Double.parseDouble(usdPrice);
        } catch (Exception e) {
            log.severe("Could not parse price from JSON: " + e.getMessage());
            return -1;
        }
    }


    @PostConstruct
    private void init() {
        fetchContainersHourlyCost();
        fetchFargatePrices();
    }

}