package com.calculator.features;

import com.calculator.infrastructure.cloud.adapters.aws.networking.AWSDataTransferCostCalculationServiceAdapter;
import com.calculator.infrastructure.web.rest.CloudServicesTCCCalculatorController;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;

import static com.calculator.application.services.calculators.CostEfficiencyCalculator.HOURS_PER_MONTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class) // Because it needs to use the lightweight test engine (no Tomcat, no Security)
@ContextConfiguration(classes = {
        CloudServicesTCCCalculatorController.class
})
public class Activity5CostOptimizationStrategiesSteps {

    @Autowired
    private CloudServicesTCCCalculatorController controller;

    private String cloudServiceParam;
    private String cloudInstanceParam;
    private String baseMonthlyCostParam;
    private String finopsStrategyParam;

    private Map<String, Object> targetInstanceData;
    private Map<String, Object> optimizationData;

    @Given("a cloud service {string} and a {string} implementation with baseline cost {string}")
    public void a_cloud_service_and_a_implementation_with_baseline_cost(String cloudService, String cloudInstance, String baseMonthlyCost) {
        this.cloudServiceParam = cloudService;
        this.cloudInstanceParam = cloudInstance;
        this.baseMonthlyCostParam = baseMonthlyCost;

        List<Map<String, Object>> ec2Instances = controller.getComputeInstances();
        assertNotNull(ec2Instances, "EC2 instances pricing list should not be null");

        this.targetInstanceData = ec2Instances.stream()
                .filter(inst -> cloudInstance.equalsIgnoreCase(String.valueOf(inst.get("instanceType"))))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Instance type not found in calculator: " + cloudInstance));

        double parsedExpectedCost = Double.parseDouble(baseMonthlyCost.replace("$", ""));
        double hourlyPrice = ((Number) targetInstanceData.get("pricePerHourUsd")).doubleValue();
        double calculatedMonthlyBase = Math.round(hourlyPrice * HOURS_PER_MONTH * 100.0) / 100.0; // 730 hours benchmark

        assertEquals(parsedExpectedCost, calculatedMonthlyBase, 0.01,
                "The computed baseline monthly cost from the controller does not match the expected Gherkin baseline.");
    }

    @When("I apply the cost optimization strategy {string}")
    public void i_apply_the_cost_optimization_strategy(String finopsStrategy) {
        this.finopsStrategyParam = finopsStrategy;

        this.optimizationData = controller.getCostOptimisationPricing();
        assertNotNull(optimizationData, "Cost optimization pricing data should not be null");
    }

    @Then("the system should predict a savings percentage of {string} in cost reduction")
    public void the_system_should_predict_a_savings_percentage_of_in_cost_reduction(String expectedSavings) {
        double expectedPct = Double.parseDouble(expectedSavings.replace("%", "").trim()) / 100.0;

        String normalizedStrategy = finopsStrategyParam.trim();

        double actualPctValue = 0.0;
        if (normalizedStrategy.equalsIgnoreCase("Reserved Instances")
                || normalizedStrategy.equalsIgnoreCase("Reserved Instance 1 year")) {

            Object rawPct = optimizationData.get("reservedInstance1yrSavingsPct");
            assertNotNull(rawPct, "reservedInstance1yrSavingsPct key should exist in optimization data map");

            actualPctValue = ((Number) rawPct).doubleValue();
        }

        double actualPct = actualPctValue / 100.0;

        assertEquals(expectedPct, actualPct, 0.001,
                String.format("The savings percentage evaluated from the controller (%s) does not match expected (%s).",
                        actualPct, expectedPct));
    }

    @Then("indicate the impact of {string} on the original tactic effectiveness")
    public void indicate_the_impact_of_on_the_original_tactic_effectiveness(String expectedImpact) {
        String riNote = String.valueOf(optimizationData.get("riNote"));
        assertNotNull(riNote, "Reserved Instance notes should not be null");

        assertTrue(riNote.equals(expectedImpact),
                String.format("Expected strategy metadata containing terms not found in controller response: %s", riNote));
    }
}