package com.calculator.infrastructure.repositories.impl;

import com.calculator.domain.model.architecture.ArchitecturalPattern;
import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.repository.secondaryports.ArchitecturalPatternRepository;
import com.calculator.infrastructure.entities.architecture.ArchitecturalPatternEntity;
import com.calculator.infrastructure.entities.architecture.ArchitecturalTacticEntity;
import com.calculator.infrastructure.repositories.jpa.ArchitecturalPatternJPARepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ArchitecturalPatternRepositoryImpl implements ArchitecturalPatternRepository {

    private final ArchitecturalPatternJPARepository architecturalPatternJPARepository;

    @Override
    public Optional<ArchitecturalPattern> getByName(String name) {
        Optional<ArchitecturalPattern> optionalPattern = Optional.empty();
        Optional<ArchitecturalPatternEntity> architecturalPatternEntity = Optional.ofNullable(
                architecturalPatternJPARepository.findByName(name)
        );
        if(architecturalPatternEntity.isPresent()) {
            ArchitecturalPattern architecturalPattern = ArchitecturalPattern
                    .builder()
                    .name(architecturalPatternEntity.get().getName())
                    .architecturalTactics(
                            setArchitecturalTactics(architecturalPatternEntity.get().getArchitecturalTactics())
                    )
                    .architecturalCharacteristic(
                            ArchitecturalCharacteristic.builder()
                                    .name(architecturalPatternEntity.get().getArchitecturalCharacteristic().getName())
                                    .build()
                    )
                    .build();
            optionalPattern = Optional.of(architecturalPattern);
        }
        return optionalPattern;
    }

    private List<ArchitecturalTactic> setArchitecturalTactics(List<ArchitecturalTacticEntity> architecturalTacticEntities) {
        List<ArchitecturalTactic> architecturalTactics = architecturalTacticEntities
                .stream()
                .map(
                        architecturalTacticEntity -> {
                            return ArchitecturalTactic.builder()
                                    .name(architecturalTacticEntity.getName())
                                    .architecturalCharacteristic(
                                            ArchitecturalCharacteristic.builder()
                                                    .name(architecturalTacticEntity.getName())
                                                    .build()
                                    )
                                    .build();
                        }
                ).toList();
        return architecturalTactics;
    }
}
