package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.ports.ALBCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.CachingCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.CloudComputeCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.DatabaseCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.FinOpsStrategyCostCalculatorPort;
import com.calculator.application.services.calculators.cost.cloud.ports.SecurityCostCalculatorPort;
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
class CloudServicesTCCCalculatorControllerTest {

    @Mock
    private CloudComputeCostCalculatorPort cloudComputeCostCalculator;

    @Mock
    private ALBCostCalculatorPort albCostCalculator;

    @Mock
    private DatabaseCostCalculatorPort databaseCostCalculator;

    @Mock
    private SecurityCostCalculatorPort securityCostCalculator;

    @Mock
    private FinOpsStrategyCostCalculatorPort finOpsStrategyCostCalculator;

    @Mock
    private CachingCostCalculatorPort cachingCostCalculator;

    @InjectMocks
    private CloudServicesTCCCalculatorController controller;

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