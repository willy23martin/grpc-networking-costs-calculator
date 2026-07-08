package com.calculator.application.services.calculators.cost.cloud.ports;

import com.calculator.domain.dto.responses.containers.ContainerPricingResponse;

public interface ContainerizedCostCalculatorPort {

    double fetchContainersHourlyCost();

    ContainerPricingResponse getContainerPricingResponse();
}
