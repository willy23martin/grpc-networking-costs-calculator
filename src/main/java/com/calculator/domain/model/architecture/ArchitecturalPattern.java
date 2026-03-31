package com.calculator.domain.model.architecture;

import com.calculator.domain.model.cost.CostFactor;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.*;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
public final class ArchitecturalPattern extends ArchitecturalDecision {

    private String name;

    private List<ArchitecturalTactic> architecturalTactics;

    @Builder // DESIGN PATTERN: BUILDER
    public ArchitecturalPattern(
            String name,
            List<ArchitecturalTactic> architecturalTactics,
            ArchitecturalCharacteristic architecturalCharacteristic,
            Map<String, CostFactor> costFactors) {
        super(architecturalCharacteristic, costFactors);
        this.name = name;
        this.architecturalTactics = architecturalTactics;
    }

}
