package com.calculator.infrastructure.repositories.jpa;

import com.calculator.infrastructure.entities.quality.ArchitecturalCharacteristicEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchitecturalCharacteristicsJPARepository extends JpaRepository<ArchitecturalCharacteristicEntity, Long> {
}
