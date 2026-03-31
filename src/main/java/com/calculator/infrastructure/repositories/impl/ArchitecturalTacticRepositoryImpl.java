package com.calculator.infrastructure.repositories.impl;

import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.repository.secondaryports.ArchitecturalTacticRepository;
import com.calculator.infrastructure.repositories.jpa.ArchitecturalTacticJPARepository;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class ArchitecturalTacticRepositoryImpl implements ArchitecturalTacticRepository {

    private final ArchitecturalTacticJPARepository architecturalTacticJPARepository;

    @Override
    public Optional<ArchitecturalTactic> retrieveByName(String name) {
        Optional<ArchitecturalTactic> optionalTactic = Optional.empty();
        var architecturalTacticEntity = Optional.ofNullable(architecturalTacticJPARepository.findByName(name));
        if(architecturalTacticEntity.isPresent()) {
            ArchitecturalTactic architecturalTactic = ArchitecturalTactic.builder()
                    .name(architecturalTacticEntity.get().getName())
                    .architecturalCharacteristic(
                            ArchitecturalCharacteristic.builder()
                                    .name(architecturalTacticEntity.get().getArchitecturalCharacteristic().getName())
                                    .build()
                    )
                    .build();
            optionalTactic = Optional.of(architecturalTactic);
        }
        return optionalTactic;
    }
}
