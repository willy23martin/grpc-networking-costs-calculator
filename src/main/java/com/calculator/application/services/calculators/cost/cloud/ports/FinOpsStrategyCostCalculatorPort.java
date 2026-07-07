package com.calculator.application.services.calculators.cost.cloud.ports;

import java.util.Map;

public interface FinOpsStrategyCostCalculatorPort {

    Map<String, Object> calculateFinOpsStrategiesCosts();

    double getOnDemandPrice(String instanceType);

    double fetchRiPrice(String instanceType, String leaseContractLength, String purchaseOption);
}
