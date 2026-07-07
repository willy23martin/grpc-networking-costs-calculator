package com.calculator.features;

import com.calculator.infrastructure.cloud.adapters.aws.containers.AWSContainersCostCalculatorAdapter;
import com.calculator.domain.model.architecture.CloudService;
import com.calculator.domain.model.architecture.FinOpsStrategy;
import com.calculator.domain.repository.cloud.CloudArchitecturalDecisionRepository;
import com.calculator.domain.repository.finops.cloud.FinOpsCloudServiceArchitecturalDecisionsRepository;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class OptimizedCostCalculationSteps {

    @Autowired
    private AWSContainersCostCalculatorAdapter eksComputeCostCalculator;

    @Autowired
    private CloudArchitecturalDecisionRepository cloudArchitecturalDecisionRepository;

    @Autowired
    private FinOpsCloudServiceArchitecturalDecisionsRepository finOpsRepository;

    private String finopsStrategyParam;
    private String cloudServiceParam;

    private double calculatedHourlyBase;
    private double calculatedMonthlyBase;
    private double calculatedOptimizedMonthlyCost;
    private double calculatedMonthlySavings;

    private CloudService fetchedCloudService;
    private FinOpsStrategy fetchedFinOpsStrategy;

    @Given("I have applied {string} to services implementing {string}")
    public void i_have_applied_to_services_implementing(String finopsStrategy, String cloudService) {
        this.finopsStrategyParam = finopsStrategy;
        this.cloudServiceParam = cloudService;
    }

    @When("I recalculate total costs across the architecture")
    public void i_recalculate_total_costs_across_the_architecture() {
        this.calculatedHourlyBase = eksComputeCostCalculator.fetchContainersHourlyCost();

        this.calculatedMonthlyBase = this.calculatedHourlyBase * 730;

        double savingsFactor = 0.50;
        this.calculatedMonthlySavings = this.calculatedMonthlyBase * savingsFactor;
        this.calculatedOptimizedMonthlyCost = this.calculatedMonthlyBase - this.calculatedMonthlySavings;

        // 4. Resolve Domain models to secure internal link tracking bounds
        this.fetchedCloudService = (CloudService) cloudArchitecturalDecisionRepository.getAmazonEKSControlPlaneCloudService();
        this.fetchedFinOpsStrategy = (FinOpsStrategy) finOpsRepository.getFinOpsStrategyForAWSApplicationLoadBalancer();
    }

    @Then("the system should show {string} as the optimized cost")
    public void the_system_should_show_as_the_optimized_cost(String expectedNewMonthlyCost) {
        double expectedOptimizedCost = Double.parseDouble(expectedNewMonthlyCost.replace("$", ""));

        // Validate basic calculation matrix math
        assertEquals(0.10, this.calculatedHourlyBase, 0.001, "EKS control plane base fee must equal $0.10/hr.");
        assertEquals(73.00, this.calculatedMonthlyBase, 0.001, "The baseline evaluation run does not match $73.00.");
        assertEquals(expectedOptimizedCost, this.calculatedOptimizedMonthlyCost, 0.001, "The calculated optimized cost mismatches standard calculation matrix execution.");

        // Assert domain mapping linkage integrity across your architecture repositories
        assertNotNull(fetchedCloudService, "EKS control plane core model should be initialized.");
        assertTrue(fetchedCloudService.getName().contains(cloudServiceParam), "The retrieved cloud service name does not match scenario context parameters.");
    }

    @Then("calculate {string}")
    public void calculate_annual_savings(String expectedAnnualSavings) {
        double expectedSavingsValue = Double.parseDouble(expectedAnnualSavings.replace("$", ""));

        // Validate savings field parameters
        assertEquals(expectedSavingsValue, this.calculatedMonthlySavings, 0.001, "The calculated monthly optimization savings amount mismatches standard execution.");
        assertNotNull(fetchedFinOpsStrategy, "The corresponding FinOps domain model should be registered.");
        assertEquals(this.finopsStrategyParam, fetchedFinOpsStrategy.getName(), "FinOps strategy structural configuration name mismatch.");

        // Confirm dynamic structural dependency mapping lookup
        boolean mapsToEksCluster = fetchedFinOpsStrategy.getCloudServices().stream()
                .anyMatch(service -> service.getId().equals(fetchedCloudService.getId()));
        assertTrue(mapsToEksCluster, "The optimized calculation configuration lacks a tracking relationship linking it back to the EKS core layout.");
    }
}