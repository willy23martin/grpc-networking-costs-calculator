package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.alb.ALBCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.caching.CachingCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.compute.CloudComputeCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.DatabaseCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.finops.FinOpsStrategyCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.security.SecurityCostCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/aws")
@CrossOrigin(origins = "*")
public class CloudServicesTCCCalculatorController {

    private static final Logger log = Logger.getLogger(CloudServicesTCCCalculatorController.class.getName());

    @Autowired
    CloudComputeCostCalculator cloudComputeCostCalculator; // Amazon EC2 for example

    @Autowired
    ALBCostCalculator albCostCalculator;

    @Autowired
    DatabaseCostCalculator databaseCostCalculator;

    @Autowired
    SecurityCostCalculator securityCostCalculator;

    @Autowired
    FinOpsStrategyCostCalculator finOpsStrategyCostCalculator;

    @Autowired
    CachingCostCalculator cachingCostCalculator;

    @GetMapping("/ec2-instances")
    public List<Map<String, Object>> getComputeInstances() {
        log.info("/api/aws/ec2-instances has been invoked");
        return cloudComputeCostCalculator.calculatePriceByComputeInstance();
    }

    @GetMapping("/alb-pricing")
    public Map<String, Object> getAlbPricing() {
        log.info("/api/aws/alb-pricing has been invoked");
        return albCostCalculator.calculateALBCosts();
    }

    @GetMapping("/database-backup-pricing")
    public Map<String, Object> getDatabaseBackupPricing() {
        log.info("/api/aws/database-backup-pricing has been invoked");
        return databaseCostCalculator.calculateDatabaseBackupPricing();
    }

    @GetMapping("/security-services")
    public Map<String, Object> getSecurityServicesPricing() {
        log.info("/api/aws/security-services-pricing has been invoked");
        return securityCostCalculator.calculateSecurityCosts();
    }

    @GetMapping("/cost-optimisation")
    public Map<String, Object> getCostOptimisationPricing() {
        log.info("/api/aws/cost-optimisation has been invoked");
        return finOpsStrategyCostCalculator.calculateFinOpsStrategiesCosts();
    }

    @GetMapping("/caching-pricing")
    public Map<String, Object> getCachingPricing() {
        log.info("/api/aws/caching-pricing has been invoked");
        return cachingCostCalculator.calculateCachingCosts();
    }
}