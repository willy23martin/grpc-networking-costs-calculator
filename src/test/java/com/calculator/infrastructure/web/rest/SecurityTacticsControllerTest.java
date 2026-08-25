package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tradeoffs.security.SecurityTradeoffMapperService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityTacticsControllerTest {

    @Mock
    private SecurityTradeoffMapperService securityTradeoffMapperService;

    @InjectMocks
    private SecurityTacticsController controller;

    @Test
    void getTacticSecurityMappings_returnsList() {
        when(securityTradeoffMapperService.getSecurityTradeoffs()).thenReturn(Collections.emptyList());
        final ResponseEntity<?> response = controller.getTacticSecurityMappings();
        assertNotNull(response.getBody());
    }

}
