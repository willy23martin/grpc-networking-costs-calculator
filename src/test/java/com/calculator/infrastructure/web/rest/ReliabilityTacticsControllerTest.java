package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tradeoffs.reliability.ReliabilityTradeoffMapperService;
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
public class ReliabilityTacticsControllerTest {

    @Mock
    private ReliabilityTradeoffMapperService reliabilityTradeoffMapperService;

    @InjectMocks
    private ReliabilityTacticsController controller;

    @Test
    void getTacticReliabilityMappings_returnsList() {
        when(reliabilityTradeoffMapperService.getReliabilityTradeoffs()).thenReturn(Collections.emptyList());
        ResponseEntity<?> response = controller.getTacticReliabilityMappings();
        assertNotNull(response.getBody());
    }

}
