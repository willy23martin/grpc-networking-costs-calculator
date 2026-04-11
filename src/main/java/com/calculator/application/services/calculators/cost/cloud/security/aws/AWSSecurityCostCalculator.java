package com.calculator.application.services.calculators.cost.cloud.security.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.security.SecurityCostCalculator;

import java.util.LinkedHashMap;
import java.util.Map;

public class AWSSecurityCostCalculator extends AWSCloudCalculator implements SecurityCostCalculator {

    @Override
    public Map<String, Object> calculateSecurityCosts() {
        Map<String, Object> result = new LinkedHashMap<>();

        // TODO Get from AWS
        // GuardDuty — per GB of CloudTrail/VPC flow logs analysed
        result.put("guardDutyPerGbLogs",       fetchSimplePrice("AmazonGuardDuty", "Logs", "Security", 1.00));
        result.put("guardDutyFirstGbFreeNote", "First 500 GB/month free. $1.00/GB thereafter (tiered).");

        // Amazon Inspector — per EC2 instance
        result.put("inspectorPerInstanceMonth", fetchSimplePrice("AmazonInspector", "EC2 Instance", "Security", 1.178));

        // AWS WAF — per WebACL + per rule + per million requests
        result.put("wafWebAclPerMonth",         fetchSimplePrice("awswaf", "WebACL", "Security", 5.00));
        result.put("wafRulePerMonth",            fetchSimplePrice("awswaf", "Rule",   "Security", 1.00));
        result.put("wafPer1MRequests",           fetchSimplePrice("awswaf", "Request","Security", 0.60));

        // Macie — per GB of S3 data classified
        result.put("maciePerGbClassified",      fetchSimplePrice("AmazonMacie", "Data Classification", "Security", 1.00));
        result.put("macieFirstGbFreeNote",      "First 1 GB/month free. $1.00/GB thereafter.");

        // CloudWatch Logs — ingestion + storage
        result.put("cloudwatchLogsIngestionPerGb", 0.50);
        result.put("cloudwatchLogsStoragePerGbMonth", 0.03);

        // AWS Audit Manager — per assessment
        result.put("auditManagerPerAssessmentMonth", fetchSimplePrice("AWSAuditManager","Assessment","Security",6.00));

        // KMS — CMK per month + per 10k API calls
        result.put("kmsCmkPerMonth",            1.00);
        result.put("kmsApiCallsPer10k",         0.03);
        result.put("kmsNote",                   "Data encryption at rest via KMS. $1/CMK/month + $0.03 per 10,000 API calls.");

        return result;
    }
}
