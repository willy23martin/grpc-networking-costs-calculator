package com.calculator.infrastructure.web.rest;

import com.calculator.application.services.calculators.cost.cloud.ports.FinOpsStrategyCostCalculatorPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

import static com.calculator.application.services.utils.MathUtils.round4;

@RestController
@RequestMapping("/api/finops")
@CrossOrigin(origins = "*")
public class FinOpsDiscountController {

    /* ── Fallback discount rates (Q1-2025) if Pricing API unavailable ── */
    private static final double RI_1YR_STD_DISCOUNT_PCT = 37.0; /* Standard RI 1-yr, no upfront */
    private static final double RI_3YR_STD_DISCOUNT_PCT = 57.0; /* Standard RI 3-yr, no upfront */
    private static final double RI_CONVERTIBLE_1YR_DISCOUNT_PCT = 28.0; /* Convertible RI 1-yr, no upfront */
    private static final double SAVINGS_PLAN_1YR_DISCOUNT_PCT = 29.0; /* Compute SP 1-yr, no upfront */
    private static final double SAVINGS_PLAN_3YR_DISCOUNT_PCT = 48.0; /* Compute SP 3-yr, no upfront */

    @Autowired
    private FinOpsStrategyCostCalculatorPort finOpsStrategyCostCalculatorPort;

    @GetMapping("/ri-prices/{instanceType}")
    public ResponseEntity<Map<String, Object>> getRiPrices(@PathVariable String instanceType) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("instanceType", instanceType);

        double onDemandPrice = finOpsStrategyCostCalculatorPort.getOnDemandPrice(instanceType);

        if (onDemandPrice <= 0) {
            // Fallback pricing strategies based on typical AWS values if AWS pricing service is offline
            double fallbackOnDemand = 0.0416; // Baseline approx for t3.medium
            response.put("source", "Local Fallback Estimates (API Down)");
            response.put("onDemandPerHourUsd", fallbackOnDemand);
            response.put("riStandard1yrPerHourUsd", round4(fallbackOnDemand * (1 - RI_1YR_STD_DISCOUNT_PCT / 100.0)));
            response.put("riStandard3yrPerHourUsd", round4(fallbackOnDemand * (1 - RI_3YR_STD_DISCOUNT_PCT / 100.0)));
            response.put("riConvertible1yrPerHourUsd", round4(fallbackOnDemand * (1 - RI_CONVERTIBLE_1YR_DISCOUNT_PCT / 100.0)));
            response.put("savingsPlan1yrPerHourUsd", round4(fallbackOnDemand * (1 - SAVINGS_PLAN_1YR_DISCOUNT_PCT / 100.0)));
            response.put("savingsPlan3yrPerHourUsd", round4(fallbackOnDemand * (1 - SAVINGS_PLAN_3YR_DISCOUNT_PCT / 100.0)));
            return ResponseEntity.ok(response);
        }

        double ri1Yr = finOpsStrategyCostCalculatorPort.fetchRiPrice(instanceType, "1yr", "No Upfront");
        double ri3Yr = finOpsStrategyCostCalculatorPort.fetchRiPrice(instanceType, "3yr", "No Upfront");

        response.put("source", "AWS Price List API (us-east-1 Live)");
        response.put("onDemandPerHourUsd", onDemandPrice);
        response.put("riStandard1yrPerHourUsd", ri1Yr > 0 ? ri1Yr : round4(onDemandPrice * (1 - RI_1YR_STD_DISCOUNT_PCT / 100.0)));
        response.put("riStandard3yrPerHourUsd", ri3Yr > 0 ? ri3Yr : round4(onDemandPrice * (1 - RI_3YR_STD_DISCOUNT_PCT / 100.0)));
        response.put("riConvertible1yrPerHourUsd", round4(onDemandPrice * (1 - RI_CONVERTIBLE_1YR_DISCOUNT_PCT / 100.0)));
        response.put("savingsPlan1yrPerHourUsd", round4(onDemandPrice * (1 - SAVINGS_PLAN_1YR_DISCOUNT_PCT / 100.0)));
        response.put("savingsPlan3yrPerHourUsd", round4(onDemandPrice * (1 - SAVINGS_PLAN_3YR_DISCOUNT_PCT / 100.0)));

        return ResponseEntity.ok(response);
    }
}