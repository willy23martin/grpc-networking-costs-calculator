package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tradeoffs.security.SecurityTradeoffMapperService;
import com.calculator.domain.dto.tactics.security.SecurityTradeoffsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
public class SecurityTacticsController {

    @Autowired
    SecurityTradeoffMapperService securityTradeoffMapperService;

    @GetMapping("/api/security/tactic-mappings")
    public ResponseEntity<List<SecurityTradeoffsDTO>> getTacticSecurityMappings() {
        System.out.println("Security Tactics Mapping has been invoked");
        List<SecurityTradeoffsDTO> securityMappings = securityTradeoffMapperService.getSecurityTradeoffs();
        return ResponseEntity.ok(securityMappings);
    }

}
