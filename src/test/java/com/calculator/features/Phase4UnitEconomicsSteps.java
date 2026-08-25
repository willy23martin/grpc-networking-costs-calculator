package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;

public class Phase4UnitEconomicsSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions response;
    private String requestBody;

    @Given("a unit economics request with networking cost {string}, cloud cost {string}, RPS {string}, consumers {string}, and ARPU {string}")
    public void configureUnitEconomicsRequest(String egress, String cloud, String rps, String consumers, String arpu) {
        this.requestBody = """
                {
                  "egressTransferCostUsd": %s,
                  "cloudInfraCostUsd": %s,
                  "finopsSavingUsd": 0.0,
                  "effectiveRps": %s,
                  "consumerCount": %s,
                  "revenuePerUserPerMonth": %s
                }
                """.formatted(egress, cloud, rps, consumers, arpu);
    }

    @When("the unit economics are calculated")
    public void calculateUnitEconomics() throws Exception {
        this.response = mockMvc.perform(post("/api/cost/unit-economics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(this.requestBody));
    }

    @Then("the response returns status 200")
    public void verifyResponseStatusIsOk() throws Exception {
        this.response.andExpect(status().isOk());
    }

    @And("the calculated gross parameters match monthly TCO {string} and annual TCO {string} with requests {string}")
    public void verifyGrossTcoParameters(String expectedMonthlyTco, String expectedAnnualTco, String expectedRequests) throws Exception {
        final double monthlyTco = Double.parseDouble(expectedMonthlyTco);
        final double annualTco = Double.parseDouble(expectedAnnualTco);
        final long requests = Long.parseLong(expectedRequests);

        this.response.andExpect(jsonPath("$.totalMonthlyTcoUsd", is(closeTo(monthlyTco, 0.01))))
                .andExpect(jsonPath("$.totalAnnualTcoUsd", is(closeTo(annualTco, 0.01))))
                .andExpect(jsonPath("$.totalMonthlyRequests", is(requests)));
    }

    @And("the unit cost parameters match cost per request {string}, cost per user month {string}, and cost per user day {string}")
    public void verifyUnitCostParameters(String expectedCostPerReq, String expectedCostUserMonth, String expectedCostUserDay) throws Exception {
        final double costPerReq = Double.parseDouble(expectedCostPerReq);
        final double costUserMonth = Double.parseDouble(expectedCostUserMonth);
        final double costUserDay = Double.parseDouble(expectedCostUserDay);

        final double reqTolerance = costPerReq < 1e-6 ? 1e-11 : 1e-6;

        this.response.andExpect(jsonPath("$.costPerRequestUsd", is(closeTo(costPerReq, reqTolerance))))
                .andExpect(jsonPath("$.costPerUserPerMonthUsd", is(closeTo(costUserMonth, 0.0001))))
                .andExpect(jsonPath("$.costPerUserPerDayUsd", is(closeTo(costUserDay, 0.00001))));
    }

    @And("the ROI metrics match total monthly revenue {string}, monthly profit {string}, monthly ROI pct {string}, break-even users {string}, and net margin per user {string}")
    public void verifyRoiMetrics(String expectedMonthlyRev, String expectedMonthlyProfit, String expectedRoiPct, String expectedBreakeven, String expectedNetMargin) throws Exception {
        final double monthlyRev = Double.parseDouble(expectedMonthlyRev);
        final double monthlyProfit = Double.parseDouble(expectedMonthlyProfit);
        final double roiPct = Double.parseDouble(expectedRoiPct);
        final int breakeven = Integer.parseInt(expectedBreakeven);
        final double netMargin = Double.parseDouble(expectedNetMargin);

        this.response.andExpect(jsonPath("$.totalMonthlyRevenueUsd", is(closeTo(monthlyRev, 0.01))))
                .andExpect(jsonPath("$.netMonthlyProfitUsd", is(closeTo(monthlyProfit, 0.01))))
                .andExpect(jsonPath("$.monthlyRoiPct", is(closeTo(roiPct, 0.01))))
                .andExpect(jsonPath("$.breakEvenUsers", is(breakeven)))
                .andExpect(jsonPath("$.netMarginPerUserMonthly", is(closeTo(netMargin, 0.0001))));
    }
}