package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.alb.ALBCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.caching.CachingCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.compute.CloudComputeCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.database.DatabaseCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.finops.FinOpsStrategyCostCalculator;
import com.calculator.application.services.calculators.cost.cloud.security.SecurityCostCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloudTCOCalculatorControllerTest {

    @Mock
    private CloudComputeCostCalculator cloudComputeCostCalculator;

    @Mock
    private ALBCostCalculator albCostCalculator;

    @Mock
    private DatabaseCostCalculator databaseCostCalculator;

    @Mock
    private SecurityCostCalculator securityCostCalculator;

    @Mock
    private FinOpsStrategyCostCalculator finOpsStrategyCostCalculator;

    @Mock
    private CachingCostCalculator cachingCostCalculator;

    @InjectMocks
    private CloudTCOCalculatorController controller;

    @Test
    void getComputeInstances_returnsList() {
        List<Map<String, Object>> expected = Collections.singletonList(Collections.singletonMap("instanceType", "t3.medium"));
        when(cloudComputeCostCalculator.calculatePriceByComputeInstance()).thenReturn(expected);

        List<Map<String, Object>> actual = controller.getComputeInstances();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals("t3.medium", actual.get(0).get("instanceType"));
    }

    @Test
    void getAlbPricing_returnsMap() {
        Map<String, Object> expected = Collections.singletonMap("fixedCost", 16.20);
        when(albCostCalculator.calculateALBCosts()).thenReturn(expected);

        Map<String, Object> actual = controller.getAlbPricing();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(16.20, actual.get("fixedCost"));
    }

    @Test
    void getDatabaseBackupPricing_returnsMap() {
        Map<String, Object> expected = Collections.singletonMap("backupCost", 0.09);
        when(databaseCostCalculator.calculateDatabaseBackupPricing()).thenReturn(expected);

        Map<String, Object> actual = controller.getDatabaseBackupPricing();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(0.09, actual.get("backupCost"));
    }

    @Test
    void getSecurityServicesPricing_returnsMap() {
        Map<String, Object> expected = Collections.singletonMap("kmsCost", 1.0);
        when(securityCostCalculator.calculateSecurityCosts()).thenReturn(expected);

        Map<String, Object> actual = controller.getSecurityServicesPricing();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(1.0, actual.get("kmsCost"));
    }

    @Test
    void getCostOptimisationPricing_returnsMap() {
        Map<String, Object> expected = Collections.singletonMap("savingsPlanDiscount", 30.0);
        when(finOpsStrategyCostCalculator.calculateFinOpsStrategiesCosts()).thenReturn(expected);

        Map<String, Object> actual = controller.getCostOptimisationPricing();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(30.0, actual.get("savingsPlanDiscount"));
    }

    @Test
    void getCachingPricing_returnsMap() {
        Map<String, Object> expected = Collections.singletonMap("elasticacheCost", 50.0);
        when(cachingCostCalculator.calculateCachingCosts()).thenReturn(expected);

        Map<String, Object> actual = controller.getCachingPricing();

        assertNotNull(actual);
        assertEquals(1, actual.size());
        assertEquals(50.0, actual.get("elasticacheCost"));
    }
}