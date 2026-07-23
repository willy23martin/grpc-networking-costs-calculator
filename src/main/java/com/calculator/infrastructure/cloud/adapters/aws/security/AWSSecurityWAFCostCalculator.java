package com.calculator.infrastructure.cloud.adapters.aws.security;

import com.calculator.domain.repository.cloud.security.CloudSecurityArchitecturalDecisionRepository;
import com.calculator.shared.JSONLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.pricing.PricingClient;
import software.amazon.awssdk.services.pricing.model.Filter;
import software.amazon.awssdk.services.pricing.model.FilterType;
import software.amazon.awssdk.services.pricing.model.GetProductsRequest;
import software.amazon.awssdk.services.pricing.model.GetProductsResponse;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.calculator.infrastructure.cloud.adapters.aws.AWSCloudCalculatorAdapter.AWS_LOCATION;
import static com.calculator.infrastructure.cloud.adapters.aws.security.AWSSecurityCostCalculatorAdapter.CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO;

@Component
public class AWSSecurityWAFCostCalculator {

    private static final Logger log = Logger.getLogger(AWSSecurityWAFCostCalculator.class.getName());
    public static final double SECURITY_CLOUD_SERVICE_WAF_WEB_ACL_PER_MONTH_VALUE = 5.00;
    public static final double SECURITY_CLOUD_SERVICE_WAF_RULE_PER_MONTH_VALUE = 1.00;
    public static final double SECURITY_CLOUD_SERVICE_WAF_MILLION_REQUESTS_CHARGES_FALLBACK_VALUE = 0.60;

    private final PricingClient pricingClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    CloudSecurityArchitecturalDecisionRepository cloudSecurityArchitecturalDecisionRepository;

    public AWSSecurityWAFCostCalculator(PricingClient pricingClient) {
        this.pricingClient = pricingClient;
    }

    public Map<String, Object> calculateSecurityCosts() {
        Map<String, Object> securityCosts = new LinkedHashMap<>();
        mapAWSWAFPerACLAndRuleAndMillionRequestsCosts(securityCosts);
        return securityCosts;
    }

    private void mapAWSWAFPerACLAndRuleAndMillionRequestsCosts(Map<String, Object> securityCosts) {

        double wafWebAclPerMonth = fetchWafPrice("WebACL", SECURITY_CLOUD_SERVICE_WAF_WEB_ACL_PER_MONTH_VALUE);
        double wafRulePerMonth = fetchWafPrice("Rule", SECURITY_CLOUD_SERVICE_WAF_RULE_PER_MONTH_VALUE);
        double wafPer1MRequests = fetchWafPrice("Request", SECURITY_CLOUD_SERVICE_WAF_MILLION_REQUESTS_CHARGES_FALLBACK_VALUE);

        securityCosts.put("wafWebAclPerMonth", wafWebAclPerMonth);
        securityCosts.put("wafRulePerMonth", wafRulePerMonth);
        securityCosts.put("wafPer1MRequests",wafPer1MRequests);

        cloudSecurityArchitecturalDecisionRepository.getAWSWAFCloudService().getCostFactor().setValue(
                SECURITY_CLOUD_SERVICE_WAF_WEB_ACL_PER_MONTH_VALUE
                + SECURITY_CLOUD_SERVICE_WAF_RULE_PER_MONTH_VALUE
                + SECURITY_CLOUD_SERVICE_WAF_MILLION_REQUESTS_CHARGES_FALLBACK_VALUE
        );
        cloudSecurityArchitecturalDecisionRepository.getAWSWAFCloudService().getCostFactor().setCostFactorNotes(
                "WebACL fee " + SECURITY_CLOUD_SERVICE_WAF_WEB_ACL_PER_MONTH_VALUE
                + "per-rule " + SECURITY_CLOUD_SERVICE_WAF_RULE_PER_MONTH_VALUE
                + "per-million-request charges." + SECURITY_CLOUD_SERVICE_WAF_MILLION_REQUESTS_CHARGES_FALLBACK_VALUE
        );

        log.info(cloudSecurityArchitecturalDecisionRepository.getAWSKMSCloudService().getId()
                + CLOUD_SERVICE_COST_FACTOR_HAS_CHANGED_TO
                + cloudSecurityArchitecturalDecisionRepository.getAWSKMSCloudService().getCostFactor());
    }

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
            log.info("GetProductsResponse for fetchWafPrice: " + resp);
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
            log.severe("Tailored WAF fetch failed for " + groupType + ": " + e.getMessage());
            return fallback;
        }
    }
}
