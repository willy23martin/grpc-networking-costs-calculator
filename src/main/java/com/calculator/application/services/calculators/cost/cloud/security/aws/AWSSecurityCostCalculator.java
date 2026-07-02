package com.calculator.application.services.calculators.cost.cloud.security.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.security.SecurityCostCalculator;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class AWSSecurityCostCalculator extends AWSCloudCalculator implements SecurityCostCalculator {

    private static final Logger log = Logger.getLogger(AWSSecurityCostCalculator.class.getName());

    @Autowired
    AWSSecurityWAFCostCalculator awswafCostCalculator;

    @Override
    public Map<String, Object> calculateSecurityCosts() {
        Map<String, Object> securityCosts = new LinkedHashMap<>();

        mapGuardDutyPerGBCloudTrailVPCForLogsAnalysedCosts(securityCosts);
        mapAmazonInspectorPerEC2Instance(securityCosts);
        securityCosts.putAll(awswafCostCalculator.calculateSecurityCosts());
        mapMaciePerGBOfS3DataClassifiedCosts(securityCosts);
        mapCloudWatchLogsIngestionAndStorageCosts(securityCosts);
        mapAWSAuditManagerPerAssessmentCosts(securityCosts);
        mapKMSCMKPerMonthAndPerTenThousendAPICallsCosts(securityCosts);

        return securityCosts;
    }

    private void mapKMSCMKPerMonthAndPerTenThousendAPICallsCosts(Map<String, Object> securityCosts) {
        securityCosts.put("kmsCmkPerMonth",            fetchSecurityServicePrice("awskms", "Customer Managed Key", 1.00));
        securityCosts.put("kmsApiCallsPer10k",         fetchSecurityServicePrice("awskms", "KMS apiKey", 0.03));
        securityCosts.put("kmsNote",                   "Data encryption at rest via KMS. Dynamic pricing fetched per CMK/month and per API call tier.");
    }

    private void mapAWSAuditManagerPerAssessmentCosts(Map<String, Object> securityCosts) {
        // Standard industry pricing fallback value ($6.00 per resource assessment per month)
        double auditManagerFallbackPrice = 6.00;

        try {
            double fetchedPrice = fetchSecurityServicePrice("AuditManager", "Resource Assessment", auditManagerFallbackPrice);
            securityCosts.put("auditManagerPerAssessmentMonth", fetchedPrice);
        } catch (Exception e) {
            log.severe("AuditManager fetch triggered an unexpected exception, falling back directly: " + e.getMessage());
            securityCosts.put("auditManagerPerAssessmentMonth", auditManagerFallbackPrice);
        }
    }

    private void mapCloudWatchLogsIngestionAndStorageCosts(Map<String, Object> securityCosts) {
        securityCosts.put("cloudwatchLogsIngestionPerGb", fetchSecurityServicePrice("AmazonCloudWatch", "PutLogEvents", 0.50));
        securityCosts.put("cloudwatchLogsStoragePerGbMonth", fetchSecurityServicePrice("AmazonCloudWatch", "ArchiveStorage", 0.03));
    }

    private void mapMaciePerGBOfS3DataClassifiedCosts(Map<String, Object> securityCosts) {
        securityCosts.put("maciePerGbClassified",      fetchSecurityServicePrice("AmazonMacie", "Data Discovery", 1.00));
        securityCosts.put("macieFirstGbFreeNote",      "First 1 GB/month free. $1.00/GB thereafter.");
    }



    private void mapAmazonInspectorPerEC2Instance(Map<String, Object> securityCosts) {
        securityCosts.put("inspectorPerInstanceMonth", fetchSecurityServicePrice("AmazonInspector", "EC2", 1.178));
    }

    private void mapGuardDutyPerGBCloudTrailVPCForLogsAnalysedCosts(Map<String, Object> securityCosts) {
        securityCosts.put("guardDutyPerGbLogs",       fetchSecurityServicePrice("AmazonGuardDuty", "Analysis", 1.00));
        securityCosts.put("guardDutyFirstGbFreeNote", "First 500 GB/month free. $1.00/GB thereafter (tiered).");
    }

    private double fetchSecurityServicePrice(String serviceCode, String lookupKeyword, double fallback) {
        try {
            List<Filter> apiFilters = new ArrayList<>();

            if ("AuditManager".equalsIgnoreCase(serviceCode) || "AWSAuditManager".equalsIgnoreCase(serviceCode)) {
                apiFilters.add(Filter.builder().type(FilterType.TERM_MATCH).field("productFamily").value("Audit Manager").build());
            } else {
                apiFilters.add(Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build());
            }

            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode(serviceCode)
                    .filters(apiFilters)
                    .formatVersion("aws_v1")
                    .maxResults(20)
                    .build();

            GetProductsResponse resp = pricingClient.getProducts(req);
            log.info("GetProductsResponse for fetchSecurityServicePrice (" + serviceCode + "): " + resp);
            JSONLogger.logAsJSON(log, resp);

            if (resp.priceList().isEmpty()) return fallback;

            String lowerKeyword = lookupKeyword.toLowerCase();

            for (String productJson : resp.priceList()) {
                if (productJson == null || productJson.isBlank()) continue;

                JsonNode root = mapper.readTree(productJson);
                String usageType = root.path("product").path("attributes").path("usagetype").asText("").toLowerCase();
                String description = root.path("product").path("attributes").path("description").asText("").toLowerCase();

                if (lookupKeyword.isBlank() || usageType.contains(lowerKeyword) || description.contains(lowerKeyword)) {
                    Iterator<JsonNode> termsIt = root.path("terms").path("OnDemand").elements();
                    if (!termsIt.hasNext()) continue;

                    Iterator<JsonNode> priceDimensionsIt = termsIt.next().path("priceDimensions").elements();
                    if (!priceDimensionsIt.hasNext()) continue;

                    double price = priceDimensionsIt.next().path("pricePerUnit").path("USD").asDouble(fallback);
                    if (price > 0) return price;
                }
            }
            return fallback;
        } catch (Exception e) {
            log.severe("Flexible fetch failed for security service " + serviceCode + ": " + e.getMessage());
            return fallback;
        }
    }

    @PostConstruct
    public void init(){
        calculateSecurityCosts();
    }
}