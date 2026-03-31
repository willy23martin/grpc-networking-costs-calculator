package com.calculator.domain.model.architecture;

import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
public final class ArchitecturalTactic extends ArchitecturalDecision {
    private String name;

    @Builder
    public ArchitecturalTactic(String name, ArchitecturalCharacteristic architecturalCharacteristic) {
        super(architecturalCharacteristic);
        this.name = name;
    }

}
