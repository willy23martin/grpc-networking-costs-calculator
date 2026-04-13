package com.calculator.application.services.calculators.cost.cloud.database;

import java.util.Map;

public interface DatabaseCostCalculator {

    Map<String, Object> calculateDatabaseBackupPricing();

}
