package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.alb.ALBCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.caching.CachingCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.compute.CloudComputeCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.DatabaseCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.finops.FinOpsStrategyCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.security.SecurityCostCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.pricing.model.*;

import java.util.*;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/aws")
public class CloudTCOCalculatorController {

    private static final Logger log = Logger.getLogger(CloudTCOCalculatorController.class.getName());

    @Autowired
    CloudComputeCostCalculator cloudComputeCostCalculator;

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
        return cloudComputeCostCalculator.calculatePriceByComputeInstance();
    }

    @GetMapping("/alb-pricing")
    public Map<String, Object> getAlbPricing() {
        return albCostCalculator.calculateALBCosts();
    }

    @GetMapping("/database-backup-pricing")
    public Map<String, Object> getDatabaseBackupPricing() {
        return databaseCostCalculator.calculateDatabaseBackupPricing();
    }

    @GetMapping("/security-services")
    public Map<String, Object> getSecurityServicesPricing() {
        return securityCostCalculator.calculateSecurityCosts();
    }

    @GetMapping("/cost-optimisation")
    public Map<String, Object> getCostOptimisationPricing() {
        return finOpsStrategyCostCalculator.calculateFinOpsStrategiesCosts();
    }

    @GetMapping("/caching-pricing")
    public Map<String, Object> getCachingPricing() {
        return cachingCostCalculator.calculateCachingCosts();
    }
}