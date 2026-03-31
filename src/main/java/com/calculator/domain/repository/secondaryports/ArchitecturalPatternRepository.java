package com.calculator.domain.repository.secondaryports;

import com.calculator.domain.model.architecture.ArchitecturalPattern;

import java.util.Optional;

public interface ArchitecturalPatternRepository {
    Optional<ArchitecturalPattern> getByName(String name);
}
