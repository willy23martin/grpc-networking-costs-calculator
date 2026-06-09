package com.calculator.domain.model.cost;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public abstract sealed class CostFactor permits InfrastructureCost, NetworkingCost {
    protected String costImpactNotes;
}
