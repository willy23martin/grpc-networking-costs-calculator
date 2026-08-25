package com.calculator.shared;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BDDTestUtils {

    public static double calculateRIYearSavingsValue(Map<String, Object> optimizationData) {
        final Object rawPct = optimizationData.get("reservedInstance1yrSavingsPct");
        assertNotNull(rawPct, "reservedInstance1yrSavingsPct key should exist in optimization metadata map");
        return ((Number) rawPct).doubleValue();
    }

}
