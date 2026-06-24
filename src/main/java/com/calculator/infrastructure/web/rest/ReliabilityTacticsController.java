package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tradeoffs.reliability.ReliabilityTradeoffMapperService;
import com.calculator.domain.dto.tactics.reliability.ReliabilityTradeoffDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

@RestController
@CrossOrigin(origins = "*")
public class ReliabilityTacticsController {

    @Autowired
    ReliabilityTradeoffMapperService reliabilityTradeoffMapperService;

    private static final java.util.logging.Logger log = Logger.getLogger(ReliabilityTacticsController.class.getName());

    @GetMapping("/api/reliability/tactic-mappings")
    public ResponseEntity<List<ReliabilityTradeoffDTO>> getTacticReliabilityMappings() {
        log.info("Reliability Tactics Mapping has been invoked");
        List<ReliabilityTradeoffDTO> reliabilityMappings = reliabilityTradeoffMapperService.getReliabilityTradeoffs();
        log.info("Reliability Mappings: \n" + reliabilityMappings);
        return ResponseEntity.ok(reliabilityMappings);
    }

}
