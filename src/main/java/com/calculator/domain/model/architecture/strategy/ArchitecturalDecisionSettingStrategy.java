package com.calculator.domain.model.architecture.strategy;

import com.calculator.domain.model.architecture.ArchitecturalDecision;

import java.util.List;

public abstract class ArchitecturalDecisionSettingStrategy<T> {
    public abstract void setArchitecturalDecisions(T architecturalDecisions, List<ArchitecturalDecision> architecturalDecisionToSetList);
}
