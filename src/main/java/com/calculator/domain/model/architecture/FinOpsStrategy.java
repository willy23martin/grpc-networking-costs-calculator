package com.calculator.domain.model.architecture;

import com.calculator.domain.model.cost.CostFactor;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public final class FinOpsStrategy extends ArchitecturalDecision {

    private String name;

    private List<CloudService> cloudServices;

    @Builder
    public FinOpsStrategy(
            String id,
            String name,
            ArchitecturalCharacteristic architecturalCharacteristic,
            CostFactor costFactor,
            List<CloudService> cloudServices
    ) {
        super(id, architecturalCharacteristic, costFactor);
        this.name = name;
        this.cloudServices = cloudServices;
    }
}
