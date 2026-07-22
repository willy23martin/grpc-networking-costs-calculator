package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.ports.ALBCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.CachingCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.CloudComputeCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.DatabaseCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.FinOpsStrategyCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.SecurityCostCalculatorPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/cloud")
@CrossOrigin(origins = "*")
public class CloudServicesTCCCalculatorController {

    private static final Logger log = Logger.getLogger(CloudServicesTCCCalculatorController.class.getName());

    @Autowired
    CloudComputeCostCalculatorPort cloudComputeCostCalculator; // Amazon EC2 for example

    @Autowired
    ALBCostCalculatorPort albCostCalculator;

    @Autowired
    DatabaseCostCalculatorPort databaseCostCalculator;

    @Autowired
    SecurityCostCalculatorPort securityCostCalculator;

    @Autowired
    FinOpsStrategyCostCalculatorPort finOpsStrategyCostCalculator;

    @Autowired
    CachingCostCalculatorPort cachingCostCalculator;

    @GetMapping("/compute-instances")
    public List<Map<String, Object>> getComputeInstances() {
        log.info("/api/cloud/compute-instances has been invoked");
        return cloudComputeCostCalculator.calculatePriceByComputeInstance();
    }

    @GetMapping("/alb-pricing")
    public Map<String, Object> getAlbPricing() {
        log.info("/api/cloud/alb-pricing has been invoked");
        return albCostCalculator.calculateALBCosts();
    }

    @GetMapping("/database-backup-pricing")
    public Map<String, Object> getDatabaseBackupPricing() {
        log.info("/api/cloud/database-backup-pricing has been invoked");
        return databaseCostCalculator.calculateDatabaseBackupPricing();
    }

    @GetMapping("/security-services")
    public Map<String, Object> getSecurityServicesPricing() {
        log.info("/api/cloud/security-services-pricing has been invoked");
        return securityCostCalculator.calculateSecurityCosts();
    }

    @GetMapping("/cost-optimisation")
    public Map<String, Object> getCostOptimisationPricing() {
        log.info("/api/cloud/cost-optimisation has been invoked");
        return finOpsStrategyCostCalculator.calculateFinOpsStrategiesCosts();
    }

    @GetMapping("/caching-pricing")
    public Map<String, Object> getCachingPricing() {
        log.info("/api/cloud/caching-pricing has been invoked");
        return cachingCostCalculator.calculateCachingCosts();
    }
}