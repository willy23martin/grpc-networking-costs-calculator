package com.calculator.infrastructure.repositories.jpa;

import com.calculator.infrastructure.entities.architecture.ArchitecturalTacticEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchitecturalTacticJPARepository extends JpaRepository<ArchitecturalTacticEntity, Long> {
    ArchitecturalTacticEntity findByName(String name);
}
