package com.calculator.domain.model.cost;

import com.calculator.domain.model.cost.networking.NetworkingCostCriteria;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public class NetworkingCostTest {

    private NetworkingCost networkingCost;

    @Test
    void operate_ShouldReturn2000_WhenBaseValueIsDefinedAs1000_AndNetworkingCostIsMultiplier(){
        this.networkingCost = NetworkingCost
                .builder()
                .criteria(NetworkingCostCriteria.REQUESTS_PER_SECOND.name())
                .value(2)
                .operation(CostOperations.MULTIPLIER.name())
                .units(CostUnits.RPS.name())
                .build();
        Assertions.assertThat(networkingCost.operate(1000L)).isEqualTo(2000L);
    }

    @Test
    void operate_ShouldReturn1000_WhenBaseValueIsDefinedAs1000_AndNetworkingCostIsDefault(){
        this.networkingCost = NetworkingCost
                .builder()
                .criteria(NetworkingCostCriteria.REQUESTS_PER_SECOND.name())
                .value(1)
                .operation(null)
                .units(CostUnits.RPS.name())
                .build();
        Assertions.assertThat(networkingCost.operate(1000L)).isEqualTo(1000L);
    }
}
