package com.calculator.domain.model.cost;

import com.calculator.domain.model.cost.networking.NetworkingCostCriteria;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
public final class NetworkingCost extends CostFactor {

    private NetworkingCostCriteria networkingCostCriteria;

    public NetworkingCost(
            NetworkingCostCriteria networkingCostCriteria,
            double value,
            String costImpactNotes
    ) {
        super(value, costImpactNotes);
        this.networkingCostCriteria = networkingCostCriteria;
    }
}
