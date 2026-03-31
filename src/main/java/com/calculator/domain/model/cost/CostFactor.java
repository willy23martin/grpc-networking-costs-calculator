package com.calculator.domain.model.cost;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract sealed class CostFactor permits InfrastructureCost, NetworkingCost {
}
