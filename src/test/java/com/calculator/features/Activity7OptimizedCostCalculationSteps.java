package com.calculator.features;

import com.calculator.infrastructure.web.rest.BaseIntegrationTest;
import com.calculator.infrastructure.web.rest.CloudServicesTCCCalculatorController;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

import static com.calculator.application.services.calculators.CostEfficiencyCalculator.HOURS_PER_MONTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Activity7OptimizedCostCalculationSteps extends BaseIntegrationTest {

    @Autowired
    private CloudServicesTCCCalculatorController controller;

    private String finopsStrategyParam;
    private String cloudServiceParam;
    private String cloudInstanceParam;

    private double calculatedHourlyBase;
    private double calculatedMonthlyBase;
    private double calculatedOptimizedMonthlyCost;
    private double calculatedMonthlySavings;
    private double calculatedAnnualSavings;

    private Map<String, Object> targetInstanceData;
    private Map<String, Object> optimizationData;

    @Given("I have applied {string} to services implementing {string} and a {string}")
    public void i_have_applied_to_services_implementing_and_a(String finopsStrategy, String cloudService, String cloudInstance) {
        this.finopsStrategyParam = finopsStrategy;
        this.cloudServiceParam = cloudService;
        this.cloudInstanceParam = cloudInstance;

        List<Map<String, Object>> ec2Instances = controller.getComputeInstances();
        assertNotNull(ec2Instances, "EC2 instances pricing list should not be null");

        this.targetInstanceData = ec2Instances.stream()
                .filter(inst -> cloudInstance.equalsIgnoreCase(String.valueOf(inst.get("instanceType"))))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Instance type not found in calculator: " + cloudInstance));

        this.calculatedHourlyBase = ((Number) targetInstanceData.get("pricePerHourUsd")).doubleValue();
        this.calculatedMonthlyBase = this.calculatedHourlyBase * HOURS_PER_MONTH;
    }

    @When("I recalculate total costs across the architecture")
    public void i_recalculate_total_costs_across_the_architecture() {
        this.optimizationData = controller.getCostOptimisationPricing();
        assertNotNull(optimizationData, "Cost optimization pricing data should not be null");

        String normalizedStrategy = finopsStrategyParam.trim();
        double savingsPct = 0.0;

        if (normalizedStrategy.equalsIgnoreCase("Reserved Instances")
                || normalizedStrategy.equalsIgnoreCase("Reserved Instance 1 year")) {

            Object rawPct = optimizationData.get("reservedInstance1yrSavingsPct");
            assertNotNull(rawPct, "reservedInstance1yrSavingsPct key should exist in optimization metadata map");
            savingsPct = ((Number) rawPct).doubleValue();
        }

        this.calculatedMonthlySavings = this.calculatedMonthlyBase * (savingsPct / 100.0);
        this.calculatedOptimizedMonthlyCost = this.calculatedMonthlyBase - this.calculatedMonthlySavings;
        this.calculatedAnnualSavings = this.calculatedMonthlySavings * 12.0;
    }

    @Then("the system should show {string} as the optimized cost")
    public void the_system_should_show_as_the_optimized_cost(String expectedNewMonthlyCost) {
        double expectedOptimizedCost = Double.parseDouble(expectedNewMonthlyCost.replace("$", "").trim());

        assertEquals(expectedOptimizedCost, this.calculatedOptimizedMonthlyCost, 0.01,
                String.format("Optimized monthly cost calculation mismatch. Expected: %s, Calculated: %s",
                        expectedOptimizedCost, this.calculatedOptimizedMonthlyCost));
    }

    @Then("calculate {string}")
    public void calculate_annual_savings(String expectedAnnualSavings) {
        double expectedSavingsValue = Double.parseDouble(expectedAnnualSavings.replace("$", "").trim());

        assertEquals(expectedSavingsValue, this.calculatedAnnualSavings, 0.01,
                String.format("Annual savings calculation mismatch. Expected: %s, Calculated: %s",
                        expectedSavingsValue, this.calculatedAnnualSavings));
    }
}