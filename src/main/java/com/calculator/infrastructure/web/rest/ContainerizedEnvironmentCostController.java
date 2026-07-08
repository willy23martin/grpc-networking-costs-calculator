package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.ports.ContainerizedCostCalculatorPort;
import com.calculator.domain.dto.responses.containers.ContainerPricingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/aws")
@CrossOrigin(origins = "*")
public class ContainerizedEnvironmentCostController {

    @Autowired
    private ContainerizedCostCalculatorPort containerizedCostCalculatorPort;

    @GetMapping("/container-pricing")
    public ResponseEntity<ContainerPricingResponse> getContainerPricing() {
        ContainerPricingResponse containerPricingResponse = containerizedCostCalculatorPort.getContainerPricingResponse();
        return ResponseEntity.ok(containerPricingResponse);
    }


}