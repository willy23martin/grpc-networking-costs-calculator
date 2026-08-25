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
import static com.calculator.shared.BDDTestUtils.calculateRIYearSavingsValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Activity6CloudServiceConfigurationSteps extends BaseIntegrationTest {

    @Autowired
    private CloudServicesTCCCalculatorController controller;

    private Map<String, Object> targetInstanceData;
    private Map<String, Object> optimizationData;

    private double calculatedHourlyBase;
    private double calculatedMonthlyBase;
    private double calculatedMonthlySavings;

    @Given("I have implemented {string} and a {string}")
    public void i_have_implemented_and_a(String cloudService, String cloudInstance) {

        final List<Map<String, Object>> ec2Instances = controller.getComputeInstances();

        this.targetInstanceData = ec2Instances.stream()
                .filter(inst -> cloudInstance.equalsIgnoreCase(String.valueOf(inst.get("instanceType"))))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Instance type not found in calculator: " + cloudInstance));

        this.calculatedHourlyBase = ((Number) targetInstanceData.get("pricePerHourUsd")).doubleValue();
        this.calculatedMonthlyBase = this.calculatedHourlyBase * HOURS_PER_MONTH;

        assertNotNull(ec2Instances, "EC2 instances pricing list should not be null");
    }

    @When("I modify the configuration with {string}")
    public void i_modify_the_configuration_with(String finopsStrategy) {

        this.optimizationData = controller.getCostOptimisationPricing();

        final String normalizedStrategy = finopsStrategy.trim();
        final double savingsPct = normalizedStrategy.equalsIgnoreCase("Reserved Instances")
                || normalizedStrategy.equalsIgnoreCase("Reserved Instance 1 year") ? calculateRIYearSavingsValue(optimizationData) : 0.0;


        this.calculatedMonthlySavings = this.calculatedMonthlyBase * (savingsPct / 100.0);
        assertNotNull(optimizationData, "Cost optimization pricing data should not be null");
    }

    @Then("the system should calculate {string}")
    public void the_system_should_calculate(String expectedMonthlySavings) {
        double expectedSavingsValue = Double.parseDouble(expectedMonthlySavings.replace("$", "").trim());

        assertEquals(expectedSavingsValue, this.calculatedMonthlySavings, 0.01,
                String.format("Monthly savings mismatch. Expected: %s, Calculated: %s",
                        expectedSavingsValue, this.calculatedMonthlySavings));
    }

}