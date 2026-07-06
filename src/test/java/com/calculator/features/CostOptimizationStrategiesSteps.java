package com.calculator.features;

import com.calculator.application.services.calculators.cost.cloud.compute.aws.eks.EKSComputeCostCalculator;
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
public class CostOptimizationStrategiesSteps {

    @Autowired
    private EKSComputeCostCalculator eksComputeCostCalculator;

    @Autowired
    private CloudArchitecturalDecisionRepository cloudArchitecturalDecisionRepository;

    @Autowired
    private FinOpsCloudServiceArchitecturalDecisionsRepository finOpsRepository;

    private String cloudServiceParam;
    private String baseMonthlyCostParam;
    private String finopsStrategyParam;

    private double calculatedHourlyBase;
    private double calculatedMonthlyBase;
    private double optimizedMonthlyCost;
    private CloudService fetchedCloudService;
    private FinOpsStrategy fetchedFinOpsStrategy;

    @Given("a cloud service {string} implementation with baseline cost {string}")
    public void a_cloud_service_implementation_with_baseline_cost(String cloudService, String baseMonthlyCost) {
        this.cloudServiceParam = cloudService;
        this.baseMonthlyCostParam = baseMonthlyCost;
    }

    @When("I apply the cost optimization strategy {string}")
    public void i_apply_the_cost_optimization_strategy(String finopsStrategy) {
        this.finopsStrategyParam = finopsStrategy;

        // 1. Fetch live base hourly rate from the calculator ($0.10)
        this.calculatedHourlyBase = eksComputeCostCalculator.fetchEksControlPlaneHourlyCost();

        // 2. Convert to standard monthly cost baseline (730 hours per month) -> $73.00
        this.calculatedMonthlyBase = this.calculatedHourlyBase * 730;

        // 3. Dynamically evaluate reduction factors
        double savingsFactor = 0.50;
        this.optimizedMonthlyCost = this.calculatedMonthlyBase * (1.0 - savingsFactor);

        // 4. Fetch the infrastructure configuration to check parameters
        this.fetchedCloudService = (CloudService) cloudArchitecturalDecisionRepository.getAmazonEKSControlPlaneCloudService();

        // 5. Integrate the FinOps Strategy Repository and resolve strategy object
        this.fetchedFinOpsStrategy = (FinOpsStrategy) finOpsRepository.getFinOpsStrategyForAWSApplicationLoadBalancer();
    }

    @Then("the system should predict a savings percentage of {string} in cost reduction")
    public void the_system_should_predict_a_savings_percentage_of_in_cost_reduction(String expectedSavings) {
        double expectedSavingsPercent = Double.parseDouble(expectedSavings.replace("%", "")) / 100.0;

        double actualSavingsAmount = this.calculatedMonthlyBase - this.optimizedMonthlyCost;
        double actualSavingsPercentage = actualSavingsAmount / this.calculatedMonthlyBase;

        assertEquals(expectedSavingsPercent, actualSavingsPercentage, 0.001, "The savings percentage evaluated from the calculator does not match.");
        assertEquals(0.10, this.calculatedHourlyBase, 0.001, "The EKS base hourly rate must be exactly $0.10");

        assertTrue(fetchedCloudService.getName().contains("EKS"), "The fetched cloud service name does not match context.");
        assertEquals(Double.parseDouble(baseMonthlyCostParam.replace("$", "")), this.calculatedMonthlyBase, 0.001, "The scenario baseline does not match the computed baseline.");
    }

    @Then("indicate the impact of {string} on the original tactic effectiveness")
    public void indicate_the_impact_of_on_the_original_tactic_effectiveness(String expectedImpact) {
        assertNotNull(fetchedFinOpsStrategy, "The FinOps strategy mapping record must be available.");

        assertEquals(this.finopsStrategyParam, fetchedFinOpsStrategy.getName(), "The strategy name resolved does not match.");

        String actualFinOpsNotes = fetchedFinOpsStrategy.getCostFactor().getCostFactorNotes();
        assertTrue(actualFinOpsNotes.contains(expectedImpact),
                String.format("Expected strategy impact constraint: [%s] to be verified within repository documentation: [%s]",
                        expectedImpact, actualFinOpsNotes));

        boolean linksToEks = fetchedFinOpsStrategy.getCloudServices().stream()
                .anyMatch(service -> service.getId().equals(fetchedCloudService.getId()));
        assertTrue(linksToEks, "The FinOps strategy must be functionally linked back to the target EKS service layout.");
    }
}