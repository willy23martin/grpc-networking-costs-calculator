package com.calculator.domain.model.architecture;

import com.calculator.domain.model.cost.CostFactor;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@SuperBuilder
public abstract sealed class ArchitecturalDecision permits ArchitecturalPattern, ArchitecturalTactic, CloudService {

    protected String id;
    protected ArchitecturalCharacteristic architecturalCharacteristic;
    protected CostFactor costFactor;

}
