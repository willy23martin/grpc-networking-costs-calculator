package com.calculator.domain.model.architecture;

import com.calculator.domain.model.cost.CostFactor;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.*;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ToString
@Getter
@Setter
public final class ArchitecturalPattern extends ArchitecturalDecision {

    private String name;
    private List<ArchitecturalTactic> architecturalTactics;

    @Builder // DESIGN PATTERN: BUILDER
    public ArchitecturalPattern(
            String id,
            String name,
            List<ArchitecturalTactic> architecturalTactics,
            ArchitecturalCharacteristic architecturalCharacteristic,
            CostFactor costFactor
    ) {
        super(id, architecturalCharacteristic, costFactor);
        this.name = name;
        this.architecturalTactics = architecturalTactics;
    }

}
