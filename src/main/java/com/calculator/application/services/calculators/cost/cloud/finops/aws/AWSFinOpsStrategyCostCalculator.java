package com.calculator.application.services.calculators.cost.cloud.finops.aws;

import com.calculator.application.services.calculators.cost.cloud.aws.AWSCloudCalculator;
import com.calculator.application.services.calculators.cost.cloud.finops.FinOpsStrategyCostCalculator;

import java.util.LinkedHashMap;
import java.util.Map;

public class AWSFinOpsStrategyCostCalculator extends AWSCloudCalculator implements FinOpsStrategyCostCalculator {

    @Override
    public Map<String, Object> calculateFinOpsStrategiesCosts() {
        Map<String, Object> finOpsStrategiesCosts = new LinkedHashMap<>();

        mapReservedInstanceCosts(finOpsStrategiesCosts);
        mapComputeSavingsPlansCosts(finOpsStrategiesCosts);
        mapEC2InstanceSavingsPlans(finOpsStrategiesCosts);
        mapTrustedAdvisorForBusinessOrEnterpriseSupport(finOpsStrategiesCosts);

        return finOpsStrategiesCosts;
    }

    private static void mapTrustedAdvisorForBusinessOrEnterpriseSupport(Map<String, Object> finOpsStrategiesCosts) {
        finOpsStrategiesCosts.put("trustedAdvisorNote","Trusted Advisor cost optimisation checks (idle resources, RI recommendations) require AWS Business or Enterprise Support ($100+/mo or 10% of monthly usage).");
        finOpsStrategiesCosts.put("businessSupportMinMonthUsd", 100);
        finOpsStrategiesCosts.put("businessSupportPctMonthlyUsage", 10);
    }

    private static void mapEC2InstanceSavingsPlans(Map<String, Object> finOpsStrategiesCosts) {
        finOpsStrategiesCosts.put("ec2SavingsPlan1yrPct", 36);
        finOpsStrategiesCosts.put("ec2SavingsPlan3yrPct", 57);
    }

    private static void mapComputeSavingsPlansCosts(Map<String, Object> finOpsStrategiesCosts) {
        finOpsStrategiesCosts.put("savingsPlan1yrSavingsPct", 31);
        finOpsStrategiesCosts.put("savingsPlan3yrSavingsPct", 50);
        finOpsStrategiesCosts.put("savingsPlanNote","They're not locked to a specific instance type. AWS automatically applies the discount to any compute usage during the commitment period. \n Savings Plans apply automatically to the highest compute usage. Commitment is $/hour not to a specific instance type.");
    }

    private static void mapReservedInstanceCosts(Map<String, Object> finOpsStrategiesCosts) {
        finOpsStrategiesCosts.put("reservedInstance1yrSavingsPct", 36);
        finOpsStrategiesCosts.put("reservedInstance3yrSavingsPct", 57);
        finOpsStrategiesCosts.put("convertibleRi1yrSavingsPct", 28);
        finOpsStrategiesCosts.put("convertibleRi3yrSavingsPct", 47);
        finOpsStrategiesCosts.put("riNote","Reserved Instances (RIs) suit predictable, constant workloads like servers that must stay active around the clock. \n Standard RIs offer the highest discount but cannot be exchanged. Convertible RIs can be exchanged for different instance families.");
    }

}
