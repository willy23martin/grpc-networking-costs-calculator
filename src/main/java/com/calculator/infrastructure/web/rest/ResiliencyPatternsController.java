package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tradeoffs.resiliency.ResiliencyTradeoffMapperService;
import com.calculator.domain.dto.tactics.resiliency.ResiliencyTradeoffDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

@RestController
@CrossOrigin(origins = "*")
public class ResiliencyPatternsController {

    @Autowired
    ResiliencyTradeoffMapperService resiliencyTradeoffMapperService;

    private static final java.util.logging.Logger log = Logger.getLogger(ResiliencyPatternsController.class.getName());

    @GetMapping("/api/resiliency/tactic-mappings")
    public ResponseEntity<List<ResiliencyTradeoffDTO>> getTacticResiliencyMappings() {
        log.info("Resiliency Tactics Mapping has been invoked");
        List<ResiliencyTradeoffDTO> reliabilityMappings = resiliencyTradeoffMapperService.getResiliencyTradeoffs();
        log.info("Resiliency mappings: \n" + reliabilityMappings);
        return ResponseEntity.ok(reliabilityMappings);
    }

}
