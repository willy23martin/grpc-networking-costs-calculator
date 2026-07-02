package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.mapper.tradeoffs.resiliency.ResiliencyTradeoffMapperService;
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
public class ResiliencyPatternsControllerTest {

    @Mock
    private ResiliencyTradeoffMapperService resiliencyTradeoffMapperService;

    @InjectMocks
    private ResiliencyPatternsController controller;

    @Test
    void getTacticResiliencyMappings_returnsList() {
        when(resiliencyTradeoffMapperService.getResiliencyTradeoffs()).thenReturn(Collections.emptyList());
        ResponseEntity<?> response = controller.getTacticResiliencyMappings();
        assertNotNull(response.getBody());
    }
}
