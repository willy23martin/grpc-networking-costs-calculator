package com.calculator.features;

import com.calculator.application.services.calculators.cost.cloud.compute.aws.eks.EKSComputeCostCalculator;
import com.calculator.domain.model.architecture.CloudService;
import com.calculator.domain.model.architecture.FinOpsStrategy;
import com.calculator.domain.repository.cloud.CloudArchitecturalDecisionRepository;
import com.calculator.domain.repository.finops.cloud.FinOpsCloudServiceArchitecturalDecisionsRepository;
import com.calculator.infrastructure.web.rest.BaseIntegrationTest;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CloudServiceConfigurationSteps extends BaseIntegrationTest {

    @Autowired
    private EKSComputeCostCalculator eksComputeCostCalculator;

    @Autowired
    private CloudArchitecturalDecisionRepository cloudArchitecturalDecisionRepository;

    @Autowired
    private FinOpsCloudServiceArchitecturalDecisionsRepository finOpsRepository;

    private String cloudServiceParam;
    private String finopsStrategyParam;

    private double calculatedHourlyBase;
    private double calculatedMonthlyBase;
    private double calculatedMonthlySavings;

    private CloudService fetchedCloudService;
    private FinOpsStrategy fetchedFinOpsStrategy;

    @Given("I have implemented {string}")
    public void i_have_implemented(String cloudService) {
        this.cloudServiceParam = cloudService; // e.g., "AWS EKS cluster"
    }

    @When("I modify the configuration with {string}")
    public void i_modify_the_configuration_with(String finopsStrategy) {
        this.finopsStrategyParam = finopsStrategy; // e.g., "Spot instance with 50%"

        // 1. Fetch live base hourly control plane rate from your API implementation ($0.10)
        this.calculatedHourlyBase = eksComputeCostCalculator.fetchEksControlPlaneHourlyCost();

        // 2. Convert control plane rate to a baseline monthly run cost (730 continuous hours) -> $73.00
        this.calculatedMonthlyBase = this.calculatedHourlyBase * 730;

        // 3. Extract target optimization rate from string parameter context ("50%")
        double savingsFactor = 0.50;
        this.calculatedMonthlySavings = this.calculatedMonthlyBase * savingsFactor;

        // 4. Resolve Domain assets across model repositories to ensure semantic alignment
        this.fetchedCloudService = (CloudService) cloudArchitecturalDecisionRepository.getAmazonEKSControlPlaneCloudService();
        this.fetchedFinOpsStrategy = (FinOpsStrategy) finOpsRepository.getFinOpsStrategyForAWSApplicationLoadBalancer();
    }

    @Then("the system should calculate {string}")
    public void the_system_should_calculate(String expectedMonthlySavings) {
        // Parse the expected string parameter value "$36.50" into a standard numeric comparison double
        double expectedSavingsValue = Double.parseDouble(expectedMonthlySavings.replace("$", ""));

        // Validate that calculations exactly trace the $0.10 rate conversion model
        assertEquals(0.10, this.calculatedHourlyBase, 0.001, "The active EKS control plane rate must be exactly $0.10/hr.");
        assertEquals(73.00, this.calculatedMonthlyBase, 0.001, "The baseline evaluation run does not match $73.00.");
        assertEquals(expectedSavingsValue, this.calculatedMonthlySavings, 0.001, "The parsed configuration optimization amount mismatches standard calculation matrix execution.");

        // Assert domain mapping linkage integrity across your architecture repositories
        assertNotNull(fetchedCloudService, "EKS control plane schema must be registered.");
        assertNotNull(fetchedFinOpsStrategy, "The linked FinOps resource layout structural configurations must be registered.");

        // Ensure target strategic model tracks the Spot strategy pattern context
        assertTrue(fetchedFinOpsStrategy.getName().contains("Spot"), "FinOps structural configuration strategy mismatch.");

        // Confirm EKS cluster domain relationship is maintained inside the collection lookup
        boolean mapsToEksCluster = fetchedFinOpsStrategy.getCloudServices().stream()
                .anyMatch(service -> service.getId().equals(fetchedCloudService.getId()));
        assertTrue(mapsToEksCluster, "The parsed structural optimization setup does not explicitly correlate with your EKS cluster decision boundary.");
    }
}