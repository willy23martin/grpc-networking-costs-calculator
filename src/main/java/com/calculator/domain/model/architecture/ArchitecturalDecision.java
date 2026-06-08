package com.calculator.domain.model.architecture;

import com.calculator.domain.model.cost.CostFactor;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
public abstract sealed class ArchitecturalDecision permits ArchitecturalTactic, ArchitecturalPattern {

    protected ArchitecturalCharacteristic architecturalCharacteristic;

    protected Map<String, CostFactor> costFactors;

}
