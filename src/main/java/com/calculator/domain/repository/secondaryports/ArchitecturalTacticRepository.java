package com.calculator.domain.repository.secondaryports;

import com.calculator.domain.model.architecture.ArchitecturalTactic;

import java.util.Optional;

public interface ArchitecturalTacticRepository {
    Optional<ArchitecturalTactic> retrieveByName(String name);
}
