package com.calculator.domain.model.architecture;

import com.calculator.domain.model.cost.CostFactor;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public final class FinOpsStrategy extends ArchitecturalDecision {

    private String name;

    @Builder
    public FinOpsStrategy(
            String id,
            String name,
            ArchitecturalCharacteristic architecturalCharacteristic,
            CostFactor costFactor
    ) {
        super(id, architecturalCharacteristic, costFactor);
        this.name = name;
    }
}
