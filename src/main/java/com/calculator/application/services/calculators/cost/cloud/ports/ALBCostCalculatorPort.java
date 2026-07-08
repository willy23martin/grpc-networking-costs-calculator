package com.calculator.application.services.calculators.cost.cloud.ports;

import java.util.Map;

public interface ALBCostCalculatorPort {

    Map<String, Object> calculateALBCosts();

}
