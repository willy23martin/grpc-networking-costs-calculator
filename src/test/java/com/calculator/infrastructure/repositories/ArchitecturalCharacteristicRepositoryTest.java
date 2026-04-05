package com.calculator.infrastructure.repositories;

import com.calculator.infrastructure.repositories.jpa.ArchitecturalCharacteristicsJPARepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ArchitecturalCharacteristicRepositoryTest {

    @Autowired
    ArchitecturalCharacteristicsJPARepository architecturalCharacteristicsJPARepository;

    @Test
    void getArchitecturalCharacteristics_ShouldReturnNotNull() {
        Assertions.assertNotNull(
                architecturalCharacteristicsJPARepository.findAll().get(0)
        );
    }

}
