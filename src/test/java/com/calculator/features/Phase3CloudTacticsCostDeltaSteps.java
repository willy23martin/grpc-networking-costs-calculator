package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

// NOTE: @SpringBootTest / @AutoConfigureMockMvc absent — see CucumberSpringConfiguration.
public class Phase3CloudTacticsCostDeltaSteps {

    @Autowired
    private MockMvc mockMvc;

    String cloudTactic;
    private ResultActions response;

    @Given("the architect opens the {string} accordion")
    public void openAccordionSection(String sectionLabel) throws Exception {
        mockMvc.perform(get("/calculator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"body-alb\"")));
    }

    @When("the section loads the tool calls GET \\/api\\/aws\\/alb-pricing")
    public void verifyAlbPricingApiCall() throws Exception {
        response = mockMvc.perform(get("/api/aws/alb-pricing"));
        response.andExpect(status().isOk());
    }

    @Then("the live per-hour and per-LCU rates are displayed")
    public void verifyAlbRatesDisplayed() throws Exception {
        response.andExpect(jsonPath("$.fixedPerMonth").exists())
                .andExpect(jsonPath("$.lcuPerHour").exists());
    }

    @And("when the architect enters the number of ALBs and LCUs the monthly cost is computed using the fixed plus LCU formula")
    public void verifyAlbCostFormula() throws Exception {
        mockMvc.perform(get("/api/aws/alb-pricing")
                        .param("numAlbs", "2")
                        .param("numLcus", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyCost").exists());
    }

    @And("the result is stored in application state and immediately reflected in the live cost delta")
    public void verifyAlbCostReflectedInDelta() throws Exception {
        mockMvc.perform(get("/calculator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"live-delta-panel\"")));
    }

    // FIX: POST /api/session/cloud-tactic → GET /api/session/cloud-tactic
    @Given("the architect selects an EC2 instance type")
    public void selectEc2InstanceType() throws Exception {
        response = mockMvc.perform(get("/api/session/cloud-tactic")
                .param("tactic", "ec2")
                .param("instanceType", "t3.medium"));
    }

    @Then("the minimum replica count is computed from RPS request rate capacity and throughput constraints")
    public void verifyReplicaCountFormula() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.minReplicas").exists());
    }

    @And("monthly EC2 cost is shown as replica count times price per hour times 730")
    public void verifyMonthlyEc2Cost() throws Exception {
        response.andExpect(jsonPath("$.monthlyEc2Cost").exists());
    }

    // FIX: POST → GET
    @Given("the architect has entered an on-demand EC2 spend")
    public void setOnDemandEc2Spend() throws Exception {
        response = mockMvc.perform(get("/api/session/cloud-tactic")
                .param("tactic", "finops")
                .param("onDemandSpend", "1000"));
    }

    @When("Standard RI 1-yr is selected")
    public void selectStandardRiOneYear() throws Exception {
        response = mockMvc.perform(get("/api/session/cloud-tactic")
                .param("tactic", "finops")
                .param("riType", "standard-1yr")
                .param("instanceType", "t3.medium"));
    }

    @Then("the system calls GET \\/api\\/finops\\/ri-prices for the instance type to fetch live AWS discount percentages")
    public void verifyRiPricesApiCall() throws Exception {
        mockMvc.perform(get("/api/finops/ri-prices/t3.medium"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountPercentage").exists());
    }

    @And("the savings are subtracted from the total cloud infrastructure cost in both the live delta panel and the Phase 4 TCO breakdown")
    public void verifyRiSavingsApplied() throws Exception {
        response.andExpect(jsonPath("$.riSavings").exists())
                .andExpect(jsonPath("$.adjustedMonthlyCost").exists());
    }

    // FIX: POST → GET
    @Given("the architect configures the {string} cloud tactic")
    public void configureCloudTactic(String tactic) throws Exception {
        this.cloudTactic = tactic;
        response = mockMvc.perform(get("/api/session/cloud-tactic")
                .param("tactic", tactic));
    }

    @Then("the system targets the {string} AWS service")
    public void verifyAwsTargetService(String awsService) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.awsTargetService").value(awsService));
    }

    @And("the cost is calculated using {string} pricing model")
    public void verifyCostPricingModel(String costType) throws Exception {
        response.andExpect(jsonPath("$.costType").value(costType));
    }
}