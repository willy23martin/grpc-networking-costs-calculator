package com.calculator.application.services.populator;

import com.calculator.application.services.populator.microservices.MicroservicesPattersPopulatorService;
import com.calculator.application.services.populator.reliability.ReliabilityTacticsPopulatorService;
import com.calculator.application.services.populator.resiliency.ResiliencyTacticsPopulatorService;
import com.calculator.application.services.populator.security.SecurityTacticsPopulatorService;
import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class TacticsPopulatorService {

    @Autowired
    ReliabilityTacticsPopulatorService reliabilityTacticsPopulatorService;

    @Autowired
    ResiliencyTacticsPopulatorService resiliencyTacticsPopulatorService;

    @Autowired
    MicroservicesPattersPopulatorService microservicesPattersPopulatorService;

    @Autowired
    SecurityTacticsPopulatorService securityTacticsPopulatorService;

    public void populateTacticsModel(
            ArchitecturalDecisionsDTO architecturalDecisionsDTO,
            List<Map<String, String>> rpsTactics,
            List<Map<String, String>> infoTactics
    ) {
        reliabilityTacticsPopulatorService.populate(architecturalDecisionsDTO.reliabilityTactics(), infoTactics);
        resiliencyTacticsPopulatorService.populate(architecturalDecisionsDTO, rpsTactics, infoTactics);
        if (architecturalDecisionsDTO.sagaPattern().microservicesSAGAPattern()) {
            microservicesPattersPopulatorService.populate(architecturalDecisionsDTO, architecturalDecisionsDTO.requestsPerSecond(), rpsTactics);
        }
        securityTacticsPopulatorService.populate(architecturalDecisionsDTO.securityTactics(), architecturalDecisionsDTO.requestsPerSecond(), infoTactics, rpsTactics);
    }
}
