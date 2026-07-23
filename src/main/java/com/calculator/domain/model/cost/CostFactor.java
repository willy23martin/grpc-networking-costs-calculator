package com.calculator.domain.model.cost;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@AllArgsConstructor
@ToString
public abstract sealed class CostFactor permits InfrastructureCost, NetworkingCost {
    protected double value;
    protected String costFactorNotes;
}
