package com.calculator.infrastructure.cloud.adapters.aws.finops;

import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
import com.calculator.application.services.calculators.cost.cloud.ports.FinOpsStrategyCostCalculatorPort;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetPriceListFileUrlRequest;
import software.amazon.awssdk.services.pricing.model.GetPriceListFileUrlResponse;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;
import software.amazon.awssdk.services.pricing.model.ListPriceListsRequest;
import software.amazon.awssdk.services.pricing.model.ListPriceListsResponse;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Logger;

public class AWSFinOpsStrategyCostCalculatorAdapter extends AWSCloudCalculatorAdapter implements FinOpsStrategyCostCalculatorPort {

    private static final Logger log = Logger.getLogger(AWSFinOpsStrategyCostCalculatorAdapter.class.getName());

    /**
     * As the t3.medium is the general purpose instance:
     * References:
     * - https://aws.amazon.com/ec2/instance-types/t3/
     * - https://docs.aws.amazon.com/ec2/latest/instancetypes/gp.html
     */
    private static final String BENCHMARK_INSTANCE_TYPE = "t3.medium";
    public static final String COMPUTE_SAVINGS_PLANS = "ComputeSavingsPlans";

    public AWSFinOpsStrategyCostCalculatorAdapter(PricingClient pricingClient) {
        super(pricingClient);
    }

    @Override
    public Map<String, Object> calculateFinOpsStrategiesCosts() {
        Map<String, Object> finOpsStrategiesCosts = new LinkedHashMap<>();

        // 1. Price List Query API (AmazonEC2)
        fetchAndMapRICosts(finOpsStrategiesCosts);

        // 2. Price List Query API (AWSPremiumSupport) - Global context
        fetchAndMapTrustedAdvisorSupportCosts(finOpsStrategiesCosts);

        // 3. Price List Bulk API (ComputeSavingsPlans)
        fetchAndMapSavingsPlansBulkMetadata(finOpsStrategiesCosts);

        return finOpsStrategiesCosts;
    }

    private void fetchAndMapSavingsPlansBulkMetadata(Map<String, Object> targetMap) {
        double compute1yr = 31;
        double compute3yr = 50;
        log.info("Initializing compute1yr: " + compute1yr);
        log.info("Initializing compute3yr: " + compute3yr);
        String pricingFileUrl = "https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/ComputeSavingsPlans/current/index.json";

        try {
            // Step 1: Discover available Bulk Price Lists
            ListPriceListsRequest listRequest = ListPriceListsRequest.builder()
                    .serviceCode("ComputeSavingsPlans")
                    .currencyCode("USD")
                    .effectiveDate(java.time.Instant.now())
                    .build();

            ListPriceListsResponse listResponse = pricingClient.listPriceLists(listRequest);
            log.info("ListPriceListsResponse received. Total files: " + listResponse.priceLists().size());

            if (listResponse.hasPriceLists() && !listResponse.priceLists().isEmpty()) {

                software.amazon.awssdk.services.pricing.model.PriceList targetPriceList = listResponse.priceLists().stream()
                        .filter(pl -> "us-east-1".equalsIgnoreCase(pl.regionCode()))
                        .findFirst()
                        .orElse(listResponse.priceLists().getFirst()); // Fallback if not found

                String priceListArn = targetPriceList.priceListArn();
                log.info("Selected Price List Region: " + targetPriceList.regionCode() + " ARN: " + priceListArn);

                // Step 2: Grab the signed CDN download URL for the target region file
                GetPriceListFileUrlRequest urlRequest = GetPriceListFileUrlRequest.builder()
                        .priceListArn(priceListArn)
                        .fileFormat("json")
                        .build();

                GetPriceListFileUrlResponse urlResponse = pricingClient.getPriceListFileUrl(urlRequest);
                if (urlResponse.url() != null) {
                    pricingFileUrl = urlResponse.url();
                    log.info("GetPriceListFileUrlResponse for fetchAndMapSavingsPlansBulkMetadata: " + urlResponse);
                    JSONLogger.logAsJSON(log, urlResponse);

                    // Step 3: Stream and Parse the remote JSON file dynamically
                    try (InputStream in = java.net.URI.create(pricingFileUrl).toURL().openStream()) {
                        JsonNode rootBulkNode = mapper.readTree(in);

                        // Track dynamic SKUs discovered in the products metadata sweep
                        String sku1yr = null;
                        String sku3yr = null;

                        /**
                         * "products" : [ {
                         * {
                         * "sku" : "BB7BKBSC6NZW7P5B",
                         * "productFamily" : "ComputeSavingsPlans",
                         * "serviceCode" : "ComputeSavingsPlans",
                         * "usageType" : "ComputeSP:1yrNoUpfront",
                         * "operation" : "",
                         * "attributes" : {
                         * "purchaseOption" : "No Upfront",
                         * "productFamily" : "ComputeSavingsPlans",
                         * "serviceCode" : "ComputeSavingsPlans",
                         * "granularity" : "hourly",
                         * "locationType" : "AWS Region",
                         * "purchaseTerm" : "1yr",
                         * "location" : "Any",
                         * "usageType" : "ComputeSP:1yrNoUpfront"
                         * }
                         * },
                         */
                        // Phase 1: Identify SKUs using attributes metadata
                        JsonNode products = rootBulkNode.path("products");
                        if (products.isArray()) {
                            for (JsonNode productNode : products) {
                                JsonNode attributes = productNode.path("attributes");
                                String productFamily = attributes.path("productFamily").asText("");
                                String location = attributes.path("location").asText("");
                                String purchaseOption = attributes.path("purchaseOption").asText("");
                                String purchaseTerm = attributes.path("purchaseTerm").asText("");

                                // Isolate global region-agnostic ComputeSavingsPlans under "No Upfront"
                                if (COMPUTE_SAVINGS_PLANS.equalsIgnoreCase(productFamily)
                                        && "Any".equalsIgnoreCase(location)
                                        && "No Upfront".equalsIgnoreCase(purchaseOption)) {

                                    String sku = productNode.path("sku").asText("");
                                    if ("1yr".equalsIgnoreCase(purchaseTerm)) {
                                        sku1yr = sku;
                                    } else if ("3yr".equalsIgnoreCase(purchaseTerm)) {
                                        sku3yr = sku;
                                    }
                                }
                            }
                        }

                        /**
                         * "terms" : {
                         *{
                         * "sku" : "8GU23DFTKP2N43SD",
                         * "description" : "1 year All Upfront Compute Savings Plan",
                         * "effectiveDate" : "2026-06-23T21:58:42Z",
                         * "leaseContractLength" : {
                         * "duration" : 1,
                         * "unit" : "year"
                         * },
                         * "rates" : [ {
                         * "discountedSku" : "22CU75SME3BFPJU8",
                         * "discountedUsageType" : "DEN1-BoxUsage:c5d.2xlarge",
                         * "discountedOperation" : "RunInstances:0004",
                         * "discountedServiceCode" : "AmazonEC2",
                         * "rateCode" : "8GU23DFTKP2N43SD.22CU75SME3BFPJU8",
                         * "unit" : "Hrs",
                         * "discountedRate" : {
                         * "price" : "1.285",
                         * "currency" : "USD"
                         * },
                         * "discountedRegionCode" : "us-west-2-den-1",
                         * "discountedInstanceType" : "c5d.2xlarge"
                         * },
                         */
                        // Phase 2: Traverse terms.savingsPlan using matched SKUs with null protection
                        JsonNode savingsPlans = rootBulkNode.path("terms").path("savingsPlan");
                        if (savingsPlans.isArray()) {
                            for (JsonNode planNode : savingsPlans) {
                                String planSku = planNode.path("sku").asText("");

                                // Null protection prevents NullPointerException when checking targets
                                boolean is1yr = (sku1yr != null && planSku.equals(sku1yr));
                                boolean is3yr = (sku3yr != null && planSku.equals(sku3yr));

                                if (is1yr || is3yr) {
                                    JsonNode rates = planNode.path("rates");
                                    if (rates.isArray()) {
                                        // Loop through all regional objects within the matrix until we isolate us-east-1
                                        for (JsonNode rateNode : rates) {
                                            String regionCode = rateNode.path("discountedRegionCode").asText("");

                                            if ("us-east-1".equalsIgnoreCase(regionCode)) {
                                                double extractedPrice = rateNode.path("discountedRate").path("price").asDouble(0.0);

                                                if (is1yr) {
                                                    compute1yr = extractedPrice;
                                                    log.info("End value of compute1yr (us-east-1): " + compute1yr);
                                                } else {
                                                    compute3yr = extractedPrice;
                                                    log.info("End value of compute3yr (us-east-1): " + compute3yr);
                                                }
                                                break; // Break loop for rates once the us-east-1 match is resolved
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception httpEx) {
                        log.warning("Downloaded bulk manifest parsing failed; applying structural fallbacks: " + httpEx.getMessage());
                    }
                }
            }
            log.info("Successfully calculated live Savings Plans values from Bulk API.");
        } catch (Exception e) {
            log.warning("Bulk API discovery failed for Savings Plans: " + e.getMessage());
        }

        targetMap.put("savingsPlan1yrSavingsPct", (int) Math.round(compute1yr));
        targetMap.put("savingsPlan3yrSavingsPct", (int) Math.round(compute3yr));
        targetMap.put("savingsPlanBulkFileUrl", pricingFileUrl);
        targetMap.put("savingsPlanNote", "Savings Plans apply automatically to the highest compute usage. Master data dynamically parsed from Bulk URL: " + pricingFileUrl);
    }

    private void fetchAndMapTrustedAdvisorSupportCosts(Map<String, Object> targetMap) {
        double minMonthUsd = 100.0;
        double pctMonthlyUsage = 10.0;

        try {
            GetProductsRequest supportRequest = GetProductsRequest.builder()
                    .serviceCode("AWSPremiumSupport")
                    .filters(Filter.builder().type(FilterType.TERM_MATCH).field("supportPlan").value("Business").build())
                    .formatVersion("aws_v1")
                    .build();

            GetProductsResponse response = pricingClient.getProducts(supportRequest);
            log.info("GetProductsResponse for fetchAndMapTrustedAdvisorSupportCosts: \n" + response);
            JSONLogger.logAsJSON(log, response);

            for (String productJson : response.priceList()) {
                if (productJson == null || productJson.isBlank()) continue;
                JsonNode root = mapper.readTree(productJson);

                JsonNode attributes = root.path("product").path("attributes");
                String minChargeStr = attributes.path("minMonthlyCharge").asText("");
                if (!minChargeStr.isBlank()) {
                    minMonthUsd = Double.parseDouble(minChargeStr.replaceAll("[^0-9.]", ""));
                }

                JsonNode terms = root.path("terms").path("OnDemand");
                if (terms.isMissingNode() || !terms.fieldNames().hasNext()) {
                    terms = root.path("terms").path("External");
                }

                if (terms.fieldNames().hasNext()) {
                    String firstKey = terms.fieldNames().next();
                    JsonNode offerNode = terms.path(firstKey);

                    JsonNode priceDimensions = offerNode.path("priceDimensions");
                    if (priceDimensions.fieldNames().hasNext()) {
                        String dimensionKey = priceDimensions.fieldNames().next();
                        JsonNode dimension = priceDimensions.path(dimensionKey);

                        double rate = dimension.path("pricePerUnit").path("USD").asDouble(0.0);
                        if (rate > 0.0) {
                            pctMonthlyUsage = rate <= 1.0 ? rate * 100.0 : rate;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warning("Failed to fetch live AWS Support pricing; applying fallback: " + e.getMessage());
        }

        targetMap.put("trustedAdvisorNote", String.format(
                "Trusted Advisor cost optimisation checks (idle resources, RI recommendations) require AWS Business or Enterprise Support ($%.0f+/mo or %.0f%% of monthly usage).",
                minMonthUsd, pctMonthlyUsage));
        targetMap.put("businessSupportMinMonthUsd", (int) Math.round(minMonthUsd));
        targetMap.put("businessSupportPctMonthlyUsage", (int) Math.round(pctMonthlyUsage));
    }

    private void fetchAndMapRICosts(Map<String, Object> targetMap) {
        double standard1yr = 37.0; // https://aws.amazon.com/ec2/pricing/reserved-instances/pricing/ for t3 medium
        double standard3yr = 57.0; // https://aws.amazon.com/ec2/pricing/reserved-instances/pricing/ for t3 medium
        double convertible1yr = 28.0; // https://aws.amazon.com/ec2/pricing/reserved-instances/pricing/ t3 medium
        double convertible3yr = 50.0; // https://aws.amazon.com/ec2/pricing/reserved-instances/pricing/ t3 medium

        try {
            GetProductsRequest riRequest = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("instanceType").value(BENCHMARK_INSTANCE_TYPE).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("productFamily").value("Compute Instance").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("operatingSystem").value("Linux").build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("tenancy").value("Shared").build()
                    )
                    .formatVersion("aws_v1")
                    .build();

            GetProductsResponse response = pricingClient.getProducts(riRequest);
            log.info("GetProductsResponse for fetchAndMapRICosts: \n" + response);
            JSONLogger.logAsJSON(log, response);

            for (String productJson : response.priceList()) {
                if (productJson == null || productJson.isBlank()) continue;
                JsonNode root = mapper.readTree(productJson);

                JsonNode attributes = root.path("product").path("attributes");
                String preInstalledSoftware = attributes.path("preInstalledSw").asText("NA");
                if (!"NA".equalsIgnoreCase(preInstalledSoftware)) {
                    continue;
                }

                JsonNode terms = root.path("terms");
                JsonNode onDemand = terms.path("OnDemand");
                if (!onDemand.fields().hasNext()) continue;

                double onDemandPrice = onDemand.fields().next().getValue().path("priceDimensions").fields().next().getValue()
                        .path("pricePerUnit").path("USD").asDouble(0.0);

                if (onDemandPrice <= 0.0) continue;

                JsonNode reserved = terms.path("Reserved");
                Iterator<Map.Entry<String, JsonNode>> fields = reserved.fields();
                while (fields.hasNext()) {
                    JsonNode termValue = fields.next().getValue();
                    JsonNode termAttributes = termValue.path("termAttributes");
                    if (!"No Upfront".equalsIgnoreCase(termAttributes.path("PurchaseOption").asText(""))) continue;

                    String length = termAttributes.path("LeaseContractLength").asText("");
                    String typeClass = termAttributes.path("OfferingClass").asText("");

                    JsonNode priceDimensions = termValue.path("priceDimensions");
                    if (!priceDimensions.fields().hasNext()) continue;
                    double riPrice = priceDimensions.fields().next().getValue().path("pricePerUnit").path("USD").asDouble(0.00);
                    double savingsPct = Math.round(((onDemandPrice - riPrice) / onDemandPrice) * 100.0);

                    if ("1yr".equalsIgnoreCase(length)) {
                        if ("standard".equalsIgnoreCase(typeClass)) standard1yr = savingsPct;
                        else if ("convertible".equalsIgnoreCase(typeClass)) convertible1yr = savingsPct;
                    } else if ("3yr".equalsIgnoreCase(length)) {
                        if ("standard".equalsIgnoreCase(typeClass)) standard3yr = savingsPct;
                        else if ("convertible".equalsIgnoreCase(typeClass)) convertible3yr = savingsPct;
                    }
                }
            }
        } catch (Exception e) {
            log.warning("Fallback triggered for RI calculation: " + e.getMessage());
        }

        targetMap.put("reservedInstance1yrSavingsPct", (int) standard1yr);
        targetMap.put("reservedInstance3yrSavingsPct", (int) standard3yr);
        targetMap.put("convertibleRi1yrSavingsPct", (int) convertible1yr);
        targetMap.put("convertibleRi3yrSavingsPct", (int) convertible3yr);
        targetMap.put("riNote", "Reserved Instances are best for predictable, steady workloads like 24/7 production servers.");
    }

    @Override
    public double getOnDemandPrice(String instanceType) {
        try (PricingClient pc = PricingClient.builder().region(Region.US_EAST_1).build()) {
            var req = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("instanceType").value(instanceType).build(),
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("productFamily").value("Compute Instance").build(),
                            Filter.builder().type("TERM_MATCH").field("operatingSystem").value("Linux").build(),
                            Filter.builder().type("TERM_MATCH").field("tenancy").value("Shared").build(),
                            Filter.builder().type("TERM_MATCH").field("capacitystatus").value("Used").build(),
                            Filter.builder().type("TERM_MATCH").field("preInstalledSw").value("NA").build()
                    ).maxResults(1).formatVersion("aws_v1").build();

            GetProductsResponse result = pc.getProducts(req);
            log.info("Reserved Instances OnDemandEC2Price: " + result);
            JSONLogger.logAsJSON(log, result);

            if (!result.priceList().isEmpty()) {
                return extractUsdPrice(mapper.readTree(result.priceList().get(0)), "OnDemand");
            }
        } catch (Exception e) {
            log.warning("Could not fetch on-demand price for " + instanceType + " : "  + e.getMessage());
        }
        return -1.0;
    }

    @Override
    public double fetchRiPrice(String instanceType, String leaseContractLength, String purchaseOption) {
        try (PricingClient pc = PricingClient.builder().region(Region.US_EAST_1).build()) {
            var req = GetProductsRequest.builder()
                    .serviceCode("AmazonEC2")
                    .filters(
                            Filter.builder().type("TERM_MATCH").field("instanceType").value(instanceType).build(),
                            Filter.builder().type("TERM_MATCH").field("location").value("US East (N. Virginia)").build(),
                            Filter.builder().type("TERM_MATCH").field("productFamily").value("Compute Instance").build(),
                            Filter.builder().type("TERM_MATCH").field("operatingSystem").value("Linux").build(),
                            Filter.builder().type("TERM_MATCH").field("tenancy").value("Shared").build(),
                            Filter.builder().type("TERM_MATCH").field("offeringClass").value("standard").build(),
                            Filter.builder().type("TERM_MATCH").field("leaseContractLength").value(leaseContractLength).build(),
                            Filter.builder().type("TERM_MATCH").field("purchaseOption").value(purchaseOption).build()
                    ).maxResults(1).formatVersion("aws_v1").build();

            var result = pc.getProducts(req);
            log.info("fetchRiPrice: " + result);
            JSONLogger.logAsJSON(log, result);

            if (!result.priceList().isEmpty()) {
                return extractUsdPrice(mapper.readTree(result.priceList().getFirst()), "Reserved");
            }
        } catch (Exception e) {
            log.severe("Could not fetch RI prices for " + instanceType +  " : " + e.getMessage());
        }
        return -1.0;
    }

    private double extractUsdPrice(JsonNode root, String termType) {
        try {
            JsonNode terms = root.path("terms").path(termType);
            Iterator<JsonNode> termIter = terms.elements();
            if (!termIter.hasNext()) return -1;
            JsonNode term = termIter.next();
            JsonNode dims = term.path("priceDimensions");
            Iterator<JsonNode> dimIter = dims.elements();
            if (!dimIter.hasNext()) return -1;
            JsonNode dim = dimIter.next();
            String usd = dim.path("pricePerUnit").path("USD").asText("0");
            return Double.parseDouble(usd);
        } catch (Exception e) {
            return -1;
        }
    }
}