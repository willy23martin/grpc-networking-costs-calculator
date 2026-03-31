package com.calculator.infrastructure.repositories;

import com.calculator.domain.model.architecture.saga.SAGATactics;
import com.calculator.infrastructure.repositories.jpa.ArchitecturalTacticJPARepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ArchitecturalTacticRepositoryTest {

    @Autowired
    ArchitecturalTacticJPARepository architecturalTacticJPARepository;

    @Test
    void getArchitecturalTacticByName_ShouldReturnNotNull_WhenNameIsSAGAPATTERNCOMPENSATINGTRANSACTIONS() {
        Assertions.assertNotNull(
                architecturalTacticJPARepository.findByName(SAGATactics.SAGA_PATTERN_COMPENSATING_TRANSACTIONS.name())
        );
    }

}
