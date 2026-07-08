package com.calculator.application.services.calculators.cost.cloud.ports;

import java.util.List;
import java.util.Map;

public interface CloudComputeCostCalculatorPort {

    List<Map<String, Object>> calculatePriceByComputeInstance();

}
