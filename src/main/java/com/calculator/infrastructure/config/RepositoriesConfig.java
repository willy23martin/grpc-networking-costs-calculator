package com.calculator.infrastructure.config;

import com.calculator.domain.repository.secondaryports.ArchitecturalCharacteristicRepository;
import com.calculator.domain.repository.secondaryports.ArchitecturalPatternRepository;
import com.calculator.domain.repository.secondaryports.ArchitecturalTacticRepository;
import com.calculator.infrastructure.repositories.impl.ArchitecturalCharacteristicRepositoryImpl;
import com.calculator.infrastructure.repositories.impl.ArchitecturalPatternRepositoryImpl;
import com.calculator.infrastructure.repositories.impl.ArchitecturalTacticRepositoryImpl;
import com.calculator.infrastructure.repositories.jpa.ArchitecturalCharacteristicsJPARepository;
import com.calculator.infrastructure.repositories.jpa.ArchitecturalPatternJPARepository;
import com.calculator.infrastructure.repositories.jpa.ArchitecturalTacticJPARepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RepositoriesConfig {

    @Autowired
    ArchitecturalTacticJPARepository architecturalTacticJPARepository;

    @Autowired
    ArchitecturalPatternJPARepository architecturalPatternJPARepository;

    @Autowired
    ArchitecturalCharacteristicsJPARepository architecturalCharacteristicsJPARepository;

    @Bean
    public ArchitecturalTacticRepository architecturalTacticRepository() {
        return new ArchitecturalTacticRepositoryImpl(architecturalTacticJPARepository);
    }

    @Bean
    public ArchitecturalPatternRepository architecturalPatternRepository() {
        return new ArchitecturalPatternRepositoryImpl(architecturalPatternJPARepository);
    }

    @Bean
    public ArchitecturalCharacteristicRepository architecturalCharacteristicRepository(){
        return new ArchitecturalCharacteristicRepositoryImpl(architecturalCharacteristicsJPARepository);
    }

}
