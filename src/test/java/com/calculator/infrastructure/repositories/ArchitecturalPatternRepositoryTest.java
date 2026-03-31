package com.calculator.infrastructure.repositories;

import com.calculator.domain.model.architecture.MicroservicesPatterns;
import com.calculator.domain.model.architecture.tactics.SAGATactics;
import com.calculator.infrastructure.repositories.jpa.ArchitecturalPatternJPARepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ArchitecturalPatternRepositoryTest {

    @Autowired
    private ArchitecturalPatternJPARepository architecturalPatternJPARepository;

    @Test
    void getArchitecturalPatternByName_ShouldReturnSAGA_WhenPatternWithThatNameExists() throws Exception {
        Assertions.assertThat(
                architecturalPatternJPARepository.findByName(MicroservicesPatterns.SAGA.name()).getArchitecturalTactics()
                        .get(0).getName()
        ).isEqualTo(SAGATactics.SAGA_PATTERN_COMPENSATING_TRANSACTIONS.name());
    }

}
