package com.calculator.domain.model.cost;

import com.calculator.domain.model.cost.networking.NetworkingCostCriteria;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public final class NetworkingCost extends CostFactor {

    private NetworkingCostCriteria networkingCostCriteria;

    public NetworkingCost(NetworkingCostCriteria networkingCostCriteria, String costImpactNotes) {
        super(costImpactNotes);
        this.networkingCostCriteria = networkingCostCriteria;
    }
}
