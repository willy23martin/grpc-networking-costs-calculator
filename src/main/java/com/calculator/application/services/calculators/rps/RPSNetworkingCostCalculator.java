package com.calculator.application.services.calculators.rps;

public interface RPSNetworkingCostCalculator<T> {

    long calculateEffectiveRequestsPerSecond(long baseRequestsPerSecond, T pattern);
}
