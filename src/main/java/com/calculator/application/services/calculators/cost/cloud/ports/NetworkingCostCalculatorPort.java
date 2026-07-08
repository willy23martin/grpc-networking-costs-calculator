package com.calculator.application.services.calculators.cost.cloud.ports;

import java.util.List;

public interface NetworkingCostCalculatorPort {

    double calculateDataTransferCost(double responseGbPerMonth);

    List<Double> getDataTransferRates();

    double[] getStandardThresholdLimits();

}
