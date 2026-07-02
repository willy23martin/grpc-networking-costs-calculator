package com.calculator.application.services.calculators.cost.cloud.compute;

import java.util.List;
import java.util.Map;

public interface CloudComputeCostCalculator {

    List<Map<String, Object>> calculatePriceByComputeInstance();

}
