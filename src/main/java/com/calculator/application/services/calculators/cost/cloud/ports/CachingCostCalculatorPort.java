package com.calculator.application.services.calculators.cost.cloud.ports;

import java.util.Map;

public interface CachingCostCalculatorPort {

    Map<String, Object> calculateCachingCosts();

}
