package com.calculator.infrastructure.cloud.adapters.aws.security;

import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter;
import com.calculator.application.services.calculators.cost.cloud.ports.SecurityCostCalculatorPort;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.services.pricing.PricingClient;
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

public class AWSSecurityCostCalculatorAdapter extends AWSCloudCalculatorAdapter implements SecurityCostCalculatorPort {

    private static final Logger log = Logger.getLogger(AWSSecurityCostCalculatorAdapter.class.getName());

    public static final String CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO = "Cloud Service cost factor has changed to: ";

    /**
     * Becuase of:
     * The cost of each API request to AWS KMS (outside of the free tier):
     *
     * Region:
     *
     * US East (N. Virginia)
     * $0.03 per 10,000 requests
     * $0.03 per 10,000 requests involving RSA 2048 keys
     *
     * Each AWS KMS key that you create in AWS KMS costs $1/month (prorated hourly).
     * The $1/month charge is the same for symmetric keys, asymmetric keys, HMAC keys, multi-Region keys (each primary and each replica multi-Region key)
     * , keys with imported key material, and KMS keys with a key origin of either AWS CloudHSM or an external key store (XKS).
     *
     * Ref: https://aws.amazon.com/kms/pricing/
     */
    public static final double SECURITY_CLOUD_SERVICE_KMS_CMK_PRICE_PER_MONTH_FALLBACK_VALUE = 1.00;
    public static final double SECURITY_CLOUD_SERVICE_KMS_API_CALLS_PER_10K_FALLBACK_VALUE = 0.03;

    /**
     * Because of: Region:
     * US East (N. Virginia)
     * Manage Logs	Cost
     * Collect (Data Ingestion)
     *     Standard	$0.50 per GB/month
     * Store (Archival)*
     *     Standard	$0.03 per GB compressed
     * Ref: https://aws.amazon.com/cloudwatch/pricing/
     */
    public static final double SECURITY_CLOUD_SERVICE_CLOUDWATCH_LOGS_INGESTION_PER_GB_FALLBACK_VALUE = 0.50;
    public static final double SECURITY_CLOUD_SERVICE_CLOUDWATCH_STORAGE_PER_GB_MONTH_FALLBACK_VALUE = 0.03;

    /**
     * Because of: Data inspected per month for automated and targeted sensitive data discovery	Pricing
     * First 50 TB / month	$1.00 per GB
     * Ref: https://aws.amazon.com/macie/pricing/
     */
    public static final double SECURITY_CLOUD_SERVICE_MACIE_FALLBACK_VALUE = 1.00;

    /**
     * Because of EC2 scanning per month (includes continual vulnerability and network reachability scans)
     * Average number of Amazon EC2 instances scanned per month using SSM-agent based scanning*	$1.2528 per instance
     * Ref: https://aws.amazon.com/inspector/pricing/
     */
    public static final double SECURITY_CLOUD_SERVICE_AMAZON_INSPECTOR_FALLBACK_VALUE = 1.2528;

    // Because of: VPC Flow Logs and DNS Query Log Analysis - First 500 GB / month	$1.00 per GB: https://aws.amazon.com/guardduty/pricing/
    public static final double SECURITY_CLOUD_SERVICE_AMAZON_GUARDUTY_FALLBACK_VALUE = 1.00;

    // Because Per 1,000 Audit Manager resource assessments per account per Region	$1.25: https://aws.amazon.com/audit-manager/pricing/
    public static final double SECURITY_CLOUD_SERVICE_AUDIT_MANAGER_FALLBACK_PRICE = 1.25;

    private final AWSSecurityWAFCostCalculator awswafCostCalculator;

    // This relationship represents a Spring service accessing a repository without violating the architecture.
    @Autowired
    private CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;

    public AWSSecurityCostCalculatorAdapter(
            PricingClient pricingClient,
            AWSSecurityWAFCostCalculator awsSecurityWAFCostCalculator
    ) {
        super(pricingClient);
        this.awswafCostCalculator = awsSecurityWAFCostCalculator;
    }

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
        double kmsCmkPricePerMonth = fetchSecurityServicePrice("awskms", "Customer Managed Key", SECURITY_CLOUD_SERVICE_KMS_CMK_PRICE_PER_MONTH_FALLBACK_VALUE);
        double kmsApiCallsPer10k = fetchSecurityServicePrice("awskms", "KMS apiKey", SECURITY_CLOUD_SERVICE_KMS_API_CALLS_PER_10K_FALLBACK_VALUE);
        securityCosts.put("kmsCmkPerMonth", kmsCmkPricePerMonth);
        securityCosts.put("kmsApiCallsPer10k", kmsApiCallsPer10k);

        String costNotes = "Data encryption at rest via KMS. Dynamic pricing fetched per CMK/month and per API call tier. \n " +
                "kmsCmkPerMonth: " + kmsCmkPricePerMonth + " kmsApiCallsPer10k: " + kmsApiCallsPer10k;
        securityCosts.put("kmsNote", costNotes);

        cloudSecurityArchitecturalDecisionRepository.getAWSKMSCloudService().getCostFactor().setValue(kmsApiCallsPer10k);
        cloudSecurityArchitecturalDecisionRepository.getAWSKMSCloudService().getCostFactor().setCostFactorNotes(costNotes);

        log.info(cloudSecurityArchitecturalDecisionRepository.getAWSKMSCloudService().getId()
                + CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO
                + cloudSecurityArchitecturalDecisionRepository.getAWSKMSCloudService().getCostFactor());

    }

    private void mapAWSAuditManagerPerAssessmentCosts(Map<String, Object> securityCosts) {
        try {
            double fetchedPrice = fetchSecurityServicePrice("AuditManager", "Resource Assessment", SECURITY_CLOUD_SERVICE_AUDIT_MANAGER_FALLBACK_PRICE);
            securityCosts.put("auditManagerPerAssessmentMonth", fetchedPrice);
            cloudSecurityArchitecturalDecisionRepository.getAWSAuditManagerCloudService().getCostFactor().setValue(fetchedPrice);

            log.info(cloudSecurityArchitecturalDecisionRepository.getAWSAuditManagerCloudService().getId()
                    + CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO
                    + cloudSecurityArchitecturalDecisionRepository.getAWSAuditManagerCloudService().getCostFactor());
        } catch (Exception e) {
            log.severe("AuditManager fetch triggered an unexpected exception, falling back directly: " + e.getMessage());
            securityCosts.put("auditManagerPerAssessmentMonth", SECURITY_CLOUD_SERVICE_AUDIT_MANAGER_FALLBACK_PRICE);
            cloudSecurityArchitecturalDecisionRepository.getAWSAuditManagerCloudService().getCostFactor().setValue(SECURITY_CLOUD_SERVICE_AUDIT_MANAGER_FALLBACK_PRICE);

            log.info(cloudSecurityArchitecturalDecisionRepository.getAWSAuditManagerCloudService().getId()
                    + CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO
                    + cloudSecurityArchitecturalDecisionRepository.getAWSAuditManagerCloudService().getCostFactor());
        }
    }

    private void mapCloudWatchLogsIngestionAndStorageCosts(Map<String, Object> securityCosts) {
        double cloudWatchLogsIngestionPricePerGb = fetchSecurityServicePrice("AmazonCloudWatch", "PutLogEvents",
                SECURITY_CLOUD_SERVICE_CLOUDWATCH_LOGS_INGESTION_PER_GB_FALLBACK_VALUE);
        securityCosts.put("cloudwatchLogsIngestionPerGb", cloudWatchLogsIngestionPricePerGb);
        double cloudwatchLogsStoragePricePerGbMonth = fetchSecurityServicePrice("AmazonCloudWatch", "ArchiveStorage",
                SECURITY_CLOUD_SERVICE_CLOUDWATCH_STORAGE_PER_GB_MONTH_FALLBACK_VALUE);
        securityCosts.put("cloudwatchLogsStoragePerGbMonth", cloudwatchLogsStoragePricePerGbMonth);
        double combinedCloudWatchRatePerGb = cloudWatchLogsIngestionPricePerGb + cloudwatchLogsStoragePricePerGbMonth;

        cloudSecurityArchitecturalDecisionRepository.getAmazonCloudWatchCloudService().getCostFactor().setValue(combinedCloudWatchRatePerGb);
        log.info(cloudSecurityArchitecturalDecisionRepository.getAmazonCloudWatchCloudService().getId()
                + CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO
                + cloudSecurityArchitecturalDecisionRepository.getAmazonCloudWatchCloudService().getCostFactor());
    }

    private void mapMaciePerGBOfS3DataClassifiedCosts(Map<String, Object> securityCosts) {
        double price = fetchSecurityServicePrice("AmazonMacie", "Data Discovery", SECURITY_CLOUD_SERVICE_MACIE_FALLBACK_VALUE);
        securityCosts.put("maciePerGbClassified", price);

        String costNotes = "First :1 GB/month free. $" + price + "/GB thereafter.";
        securityCosts.put("macieFirstGbFreeNote", costNotes);

        cloudSecurityArchitecturalDecisionRepository.getAmazonMacieCloudService().getCostFactor().setValue(price);
        cloudSecurityArchitecturalDecisionRepository.getAmazonMacieCloudService().getCostFactor().setCostFactorNotes(costNotes);

        log.info(cloudSecurityArchitecturalDecisionRepository.getAmazonMacieCloudService().getId()
                + CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO
                + cloudSecurityArchitecturalDecisionRepository.getAmazonMacieCloudService().getCostFactor());
    }

    private void mapAmazonInspectorPerEC2Instance(Map<String, Object> securityCosts) {
        double price = fetchSecurityServicePrice("AmazonInspector", "EC2", SECURITY_CLOUD_SERVICE_AMAZON_INSPECTOR_FALLBACK_VALUE);
        securityCosts.put("inspectorPerInstanceMonth", price);
        cloudSecurityArchitecturalDecisionRepository.getAmazonInspectorCloudService().getCostFactor().setValue(price);

        log.info(cloudSecurityArchitecturalDecisionRepository.getAmazonInspectorCloudService().getId()
                + CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO
                + cloudSecurityArchitecturalDecisionRepository.getAmazonInspectorCloudService().getCostFactor());
    }

    private void mapGuardDutyPerGBCloudTrailVPCForLogsAnalysedCosts(Map<String, Object> securityCosts) {
        double price = fetchSecurityServicePrice("AmazonGuardDuty", "Analysis", SECURITY_CLOUD_SERVICE_AMAZON_GUARDUTY_FALLBACK_VALUE);
        securityCosts.put("guardDutyPerGbLogs", price);

        String costNotes = "First 500 GB/month free. $" + price + "/GB thereafter (tiered).";
        securityCosts.put("guardDutyFirstGbFreeNote", costNotes);
        cloudSecurityArchitecturalDecisionRepository.getAmazonGuardDutyCloudService().getCostFactor().setValue(price);
        cloudSecurityArchitecturalDecisionRepository.getAmazonGuardDutyCloudService().getCostFactor().setCostFactorNotes(costNotes);

        log.info(cloudSecurityArchitecturalDecisionRepository.getAmazonGuardDutyCloudService().getId()
                + CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO
                + cloudSecurityArchitecturalDecisionRepository.getAmazonGuardDutyCloudService().getCostFactor());
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
    private void init(){
        calculateSecurityCosts();
    }
}