package com.calculator.domain.model.cost;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
public final class InfrastructureCost extends CostFactor {
    public InfrastructureCost(double value, String costImpactNotes) {
        super(value, costImpactNotes);
    }
}
