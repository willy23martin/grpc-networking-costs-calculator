package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tradeoffs.security.SecurityTradeoffMapperService;
import com.calculator.domain.dto.tactics.security.SecurityTradeoffsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.logging.Logger;

@RestController
@CrossOrigin(origins = "*")
public class SecurityTacticsController {

    @Autowired
    SecurityTradeoffMapperService securityTradeoffMapperService;

    private static final java.util.logging.Logger log = Logger.getLogger(SecurityTacticsController.class.getName());

    @GetMapping("/api/security/tactic-mappings")
    public ResponseEntity<List<SecurityTradeoffsDTO>> getTacticSecurityMappings() {
        log.info("Security Tactics Mapping has been invoked");

        List<SecurityTradeoffsDTO> securityMappings = securityTradeoffMapperService.getSecurityTradeoffs();

        log.info("Security Mappings: " + securityMappings);

        return ResponseEntity.ok(securityMappings);
    }

}
