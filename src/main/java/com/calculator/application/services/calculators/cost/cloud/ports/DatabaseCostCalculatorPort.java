package com.calculator.application.services.calculators.cost.cloud.ports;

import java.util.Map;

public interface DatabaseCostCalculatorPort {

    Map<String, Object> calculateDatabaseBackupPricing();

}
