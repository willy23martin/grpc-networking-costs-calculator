package com.calculator.application.services.calculators.cost.cloud.finops.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.finops.FinOpsStrategyCostCalculator;

import java.util.LinkedHashMap;
import java.util.Map;

public class AWSFinOpsStrategyCostCalculator extends AWSCloudCalculator implements FinOpsStrategyCostCalculator {

    @Override
    public Map<String, Object> calculateFinOpsStrategiesCosts() {
        Map<String, Object> result = new LinkedHashMap<>();

        // Reserved Instance savings vs On-Demand (typical 1-yr no upfront)
        result.put("reservedInstance1yrSavingsPct",  36);
        result.put("reservedInstance3yrSavingsPct",  57);
        result.put("convertibleRi1yrSavingsPct",     28);
        result.put("convertibleRi3yrSavingsPct",     47);
        result.put("riNote","Standard RIs offer the highest discount but cannot be exchanged. Convertible RIs can be exchanged for different instance families.");

        // Compute Savings Plans (covers EC2 + Lambda + Fargate)
        result.put("savingsPlan1yrSavingsPct", 31);
        result.put("savingsPlan3yrSavingsPct", 50);
        result.put("savingsPlanNote","Savings Plans apply automatically to the highest compute usage. Commitment is $/hour not to a specific instance type.");

        // EC2 Instance Savings Plans
        result.put("ec2SavingsPlan1yrPct", 36);
        result.put("ec2SavingsPlan3yrPct", 57);

        // Trusted Advisor — available with Business/Enterprise Support
        result.put("trustedAdvisorNote","Trusted Advisor cost optimisation checks (idle resources, RI recommendations) require AWS Business or Enterprise Support ($100+/mo or 10% of monthly usage).");
        result.put("businessSupportMinMonthUsd", 100);
        result.put("businessSupportPctMonthlyUsage", 10);

        return result;
    }

}
