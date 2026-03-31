package com.calculator.domain.model.architecture;

import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public sealed class ArchitecturalDecision permits ArchitecturalTactic, ArchitecturalPattern {

    protected ArchitecturalCharacteristic architecturalCharacteristic;

}
