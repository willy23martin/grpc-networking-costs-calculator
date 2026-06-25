package com.calculator.application.services.calculators.cost.cloud.security.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.security.SecurityCostCalculator;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class AWSSecurityCostCalculator extends AWSCloudCalculator implements SecurityCostCalculator {

    private static final Logger log = Logger.getLogger(AWSSecurityCostCalculator.class.getName());

    @PostConstruct
    @Override
    public Map<String, Object> calculateSecurityCosts() {
        Map<String, Object> securityCosts = new LinkedHashMap<>();

        mapGuardDutyPerGBCloudTrailVPCForLogsAnalysedCosts(securityCosts);
        mapAmazonInspectorPerEC2Instance(securityCosts);
        mapAWSWAFPerACLAndRuleAndMillionRequestsCosts(securityCosts);
        mapMaciePerGBOfS3DataClassifiedCosts(securityCosts);
        mapCloudWatchLogsIngestionAndStorageCosts(securityCosts);
        mapAWSAuditManagerPerAssessmentCosts(securityCosts);
        mapKMSCMKPerMonthAndPerTenThousendAPICallsCosts(securityCosts);

        return securityCosts;
    }

    private void mapKMSCMKPerMonthAndPerTenThousendAPICallsCosts(Map<String, Object> securityCosts) {
        // AWS KMS service code is "awskms"
        securityCosts.put("kmsCmkPerMonth",            fetchSimplePrice(log, "awskms", "Encryption Key", 1.00));
        securityCosts.put("kmsApiCallsPer10k",         fetchSimplePrice(log, "awskms", "API Calls", 0.03));
        securityCosts.put("kmsNote",                   "Data encryption at rest via KMS. Dynamic pricing fetched per CMK/month and per API call tier.");
    }

    private void mapAWSAuditManagerPerAssessmentCosts(Map<String, Object> securityCosts) {
        // Service Code: "AWSAuditManager", Product Family: "Audit Manager"
        securityCosts.put("auditManagerPerAssessmentMonth", fetchSimplePrice(log, "AWSAuditManager", "Audit Manager", 6.00));
    }

    private void mapCloudWatchLogsIngestionAndStorageCosts(Map<String, Object> securityCosts) {
        // Amazon CloudWatch service code is "AmazonCloudWatch"
        securityCosts.put("cloudwatchLogsIngestionPerGb", fetchSimplePrice(log, "AmazonCloudWatch", "Log Ingestion", 0.50));
        securityCosts.put("cloudwatchLogsStoragePerGbMonth", fetchSimplePrice(log, "AmazonCloudWatch", "Storage Snapshot", 0.03));
    }

    private void mapMaciePerGBOfS3DataClassifiedCosts(Map<String, Object> securityCosts) {
        // AWS Macie service code is "AmazonMacie"
        securityCosts.put("maciePerGbClassified",      fetchSimplePrice(log, "AmazonMacie", "Data Classification", 1.00));
        securityCosts.put("macieFirstGbFreeNote",      "First 1 GB/month free. $1.00/GB thereafter.");
    }

    private void mapAWSWAFPerACLAndRuleAndMillionRequestsCosts(Map<String, Object> securityCosts) {
        // AWS WAF requires specialized filter parsing because filtering relies on the 'group' attribute
        securityCosts.put("wafWebAclPerMonth",         fetchWafPrice("WebACL", 5.00));
        securityCosts.put("wafRulePerMonth",           fetchWafPrice("Rule", 1.00));
        securityCosts.put("wafPer1MRequests",          fetchWafPrice("Request", 0.60));
    }

    private void mapAmazonInspectorPerEC2Instance(Map<String, Object> securityCosts) {
        // Amazon Inspector family mapping is "System Management"
        securityCosts.put("inspectorPerInstanceMonth", fetchSimplePrice(log, "AmazonInspector", "System Management", 1.178));
    }

    private void mapGuardDutyPerGBCloudTrailVPCForLogsAnalysedCosts(Map<String, Object> securityCosts) {
        // Amazon GuardDuty family mapping is "Security"
        securityCosts.put("guardDutyPerGbLogs",       fetchSimplePrice(log, "AmazonGuardDuty", "Security", 1.00));
        securityCosts.put("guardDutyFirstGbFreeNote", "First 500 GB/month free. $1.00/GB thereafter (tiered).");
    }

    /**
     * Tailored API caller for AWS WAF using modern non-deprecated Jackson extraction via .elements()
     */
    private double fetchWafPrice(String groupType, double fallback) {
        try {
            GetProductsRequest req = GetProductsRequest.builder()
                    .serviceCode("AWSWAF")
                    .filters(
                            Filter.builder().type(FilterType.TERM_MATCH).field("location").value(AWS_LOCATION).build(),
                            Filter.builder().type(FilterType.TERM_MATCH).field("group").value(groupType).build()
                    )
                    .formatVersion("aws_v1").maxResults(1).build();

            GetProductsResponse resp = pricingClient.getProducts(req);
            log.info("GetProductsResponse  for fetchWafPrice: " + resp);
            JSONLogger.logAsJSON(log, resp);

            if (resp.priceList().isEmpty()) return fallback;

            JsonNode root = mapper.readTree(resp.priceList().getFirst());

            Iterator<JsonNode> termsIt = root.path("terms").path("OnDemand").elements();
            if (!termsIt.hasNext()) return fallback;

            Iterator<JsonNode> priceDimensionsIt = termsIt.next().path("priceDimensions").elements();
            if (!priceDimensionsIt.hasNext()) return fallback;

            double price = priceDimensionsIt.next().path("pricePerUnit").path("USD").asDouble(fallback);
            return price > 0 ? price : fallback;

        } catch (Exception e) {
            log.warning("Tailored WAF fetch failed for " + groupType + ": " + e.getMessage());
            return fallback;
        }
    }
}