package com.calculator.domain.model.cost;

public sealed class CostFactor permits InfrastructureCost, NetworkingCost {
    private String criteria;
    private long value;
    private String units;
}
