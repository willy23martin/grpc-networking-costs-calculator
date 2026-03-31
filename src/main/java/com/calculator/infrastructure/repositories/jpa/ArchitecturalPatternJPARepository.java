package com.calculator.infrastructure.repositories.jpa;

import com.calculator.infrastructure.entities.architecture.ArchitecturalPatternEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchitecturalPatternJPARepository extends JpaRepository<ArchitecturalPatternEntity, Long> {
    ArchitecturalPatternEntity findByName(String name);
}
