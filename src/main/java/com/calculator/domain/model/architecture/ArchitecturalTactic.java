package com.calculator.domain.model.architecture;

import com.calculator.domain.model.cost.CostFactor;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
public final class ArchitecturalTactic extends ArchitecturalDecision {
    private String name;

    @Builder
    public ArchitecturalTactic(
            String name,
            ArchitecturalCharacteristic architecturalCharacteristic,
            Map<String, CostFactor> costFactors) {
        super(architecturalCharacteristic, costFactors);
        this.name = name;
    }

}
