package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tradeoffs.resiliency.ResiliencyTradeoffMapperService;
import com.calculator.domain.dto.tactics.resiliency.ResiliencyTradeoffDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class ResiliencyPatternsController {

    @Autowired
    ResiliencyTradeoffMapperService resiliencyTradeoffMapperService;


    @GetMapping("/api/resiliency/tactic-mappings")
    public ResponseEntity<List<ResiliencyTradeoffDTO>> getTacticResiliencyMappings() {
        System.out.println("Resiliency Tactics Mapping has been invoked");
        List<ResiliencyTradeoffDTO> reliabilityMappings = resiliencyTradeoffMapperService.getResiliencyTradeoffs();
        return ResponseEntity.ok(reliabilityMappings);
    }

}
