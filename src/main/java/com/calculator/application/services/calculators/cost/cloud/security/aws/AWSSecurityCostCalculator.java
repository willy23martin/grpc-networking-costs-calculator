package com.calculator.application.services.calculators.cost.cloud.security.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.security.SecurityCostCalculator;

import java.util.LinkedHashMap;
import java.util.Map;

public class AWSSecurityCostCalculator extends AWSCloudCalculator implements SecurityCostCalculator {

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

    private static void mapKMSCMKPerMonthAndPerTenThousendAPICallsCosts(Map<String, Object> securityCosts) {
        securityCosts.put("kmsCmkPerMonth",            1.00);
        securityCosts.put("kmsApiCallsPer10k",         0.03);
        securityCosts.put("kmsNote",                   "Data encryption at rest via KMS. $1/CMK/month + $0.03 per 10,000 API calls.");
    }

    private void mapAWSAuditManagerPerAssessmentCosts(Map<String, Object> securityCosts) {
        securityCosts.put("auditManagerPerAssessmentMonth", fetchSimplePrice("AWSAuditManager","Assessment", 6.00));
    }

    private static void mapCloudWatchLogsIngestionAndStorageCosts(Map<String, Object> securityCosts) {
        securityCosts.put("cloudwatchLogsIngestionPerGb", 0.50);
        securityCosts.put("cloudwatchLogsStoragePerGbMonth", 0.03);
    }

    private void mapMaciePerGBOfS3DataClassifiedCosts(Map<String, Object> securityCosts) {
        securityCosts.put("maciePerGbClassified",      fetchSimplePrice("AmazonMacie", "Data Classification", 1.00));
        securityCosts.put("macieFirstGbFreeNote",      "First 1 GB/month free. $1.00/GB thereafter.");
    }

    private void mapAWSWAFPerACLAndRuleAndMillionRequestsCosts(Map<String, Object> securityCosts) {
        securityCosts.put("wafWebAclPerMonth",         fetchSimplePrice("awswaf", "WebACL", 5.00));
        securityCosts.put("wafRulePerMonth",            fetchSimplePrice("awswaf", "Rule", 1.00));
        securityCosts.put("wafPer1MRequests",           fetchSimplePrice("awswaf", "Request", 0.60));
    }

    private void mapAmazonInspectorPerEC2Instance(Map<String, Object> securityCosts) {
        securityCosts.put("inspectorPerInstanceMonth", fetchSimplePrice("AmazonInspector", "EC2 Instance", 1.178));
    }

    private void mapGuardDutyPerGBCloudTrailVPCForLogsAnalysedCosts(Map<String, Object> securityCosts) {
        securityCosts.put("guardDutyPerGbLogs",       fetchSimplePrice("AmazonGuardDuty", "Logs", 1.00));
        securityCosts.put("guardDutyFirstGbFreeNote", "First 500 GB/month free. $1.00/GB thereafter (tiered).");
    }
}
