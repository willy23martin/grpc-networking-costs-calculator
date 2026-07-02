package com.calculator.domain.model.cost;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public final class InfrastructureCost extends CostFactor {
    public InfrastructureCost(String costImpactNotes) {
        super(costImpactNotes);
    }
}
