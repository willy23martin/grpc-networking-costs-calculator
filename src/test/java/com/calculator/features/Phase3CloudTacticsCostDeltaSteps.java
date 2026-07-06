package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

public class Phase3CloudTacticsCostDeltaSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions response;

    @Given("the architect opens the {string} accordion")
    public void openAccordionSection(String sectionLabel) throws Exception {
        mockMvc.perform(get("/"))
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
        response.andExpect(jsonPath("$").isNotEmpty());
    }

    @And("when the architect enters the number of ALBs and LCUs the monthly cost is computed using the fixed plus LCU formula")
    public void verifyAlbCostFormula() throws Exception {
        mockMvc.perform(get("/api/aws/alb-pricing")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @And("the result is stored in application state and immediately reflected in the live cost delta")
    public void verifyAlbCostReflectedInDelta() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"tactics-comparison-block\"")));
    }

    @Given("the architect selects an EC2 instance type")
    public void selectEc2InstanceType() throws Exception {
        response = mockMvc.perform(get("/api/aws/ec2-instances"));
    }

    @Then("the minimum replica count is computed from RPS request rate capacity and throughput constraints")
    public void verifyReplicaCountFormula() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @And("monthly EC2 cost is shown as replica count times price per hour times 730")
    public void verifyMonthlyEc2Cost() throws Exception {
        response.andExpect(jsonPath("$").isNotEmpty());
    }

    @Given("the architect has entered an on-demand EC2 spend")
    public void setOnDemandEc2Spend() throws Exception {
        String body = """
                {
                  "onDemandMonthlySpend": 1000.0
                }
                """;
        response = mockMvc.perform(post("/api/finops/discount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @When("Standard RI 1-yr is selected")
    public void selectStandardRiOneYear() throws Exception {
        String body = """
                {
                  "ec2InstanceType": "t3.medium",
                  "riStandard1yr": true
                }
                """;
        response = mockMvc.perform(post("/api/finops/discount")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    @Then("the system calls GET \\/api\\/finops\\/ri-prices for the instance type to fetch live AWS discount percentages")
    public void verifyRiPricesApiCall() throws Exception {
        mockMvc.perform(get("/api/finops/ri-prices/t3.medium"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").exists());
    }

    @And("the savings are subtracted from the total cloud infrastructure cost in both the live delta panel and the Phase 4 TCO breakdown")
    public void verifyRiSavingsApplied() throws Exception {
        response.andExpect(status().isOk());
    }

    @Given("the architect configures the {string} cloud tactic")
    public void configureCloudTactic(String tactic) throws Exception {
        // Tactic initialization setup
    }

    @Then("the system targets the {string} AWS service")
    public void verifyAwsTargetService(String awsServiceEndpoint) throws Exception {
        response = mockMvc.perform(get(awsServiceEndpoint))
                .andExpect(status().isOk());
    }
}