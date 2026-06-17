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
public class Phase3SagaEgressCostSteps {

    @Autowired
    private MockMvc mockMvc;

    private int baseRps;
    private int sagaSteps;
    private boolean exitsVpc;
    private ResultActions response;

    @Given("all SAGA steps call services within the same AWS VPC")
    public void setSagaIntraVpc() {
        this.exitsVpc = false;
    }

    // FIX: POST /api/session/tactics → GET /api/session/tactics
    @When("I configure {int} SAGA steps and leave {string} unchecked")
    public void configureSagaStepsIntraVpc(Integer steps, String checkboxLabel) throws Exception {
        this.sagaSteps = steps;
        response = mockMvc.perform(get("/api/session/tactics")
                .param("tactic", "saga")
                .param("sagaSteps", String.valueOf(steps))
                .param("exitsVpc", "false"));
    }

    @Then("the live delta shows $0 additional egress")
    public void verifyZeroEgressDelta() throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.additionalEgressCost").value(0));
    }

    @And("the tactic appears as {string}")
    public void verifyTacticLabel(String expectedLabel) throws Exception {
        response.andExpect(jsonPath("$.tacticLabel").value(expectedLabel));
    }

    @Given("the base RPS is {int} req/s and {int} SAGA steps exit the VPC")
    public void setBaseRpsAndSagaStepsExitingVpc(Integer rps, Integer steps) {
        this.baseRps = rps;
        this.sagaSteps = steps;
        this.exitsVpc = true;
    }

    // FIX: POST → GET
    @When("the {string} checkbox is enabled")
    public void enableExitsVpcCheckbox(String checkboxLabel) throws Exception {
        response = mockMvc.perform(get("/api/session/tactics")
                .param("tactic", "saga")
                .param("baseRps", String.valueOf(baseRps))
                .param("sagaSteps", String.valueOf(sagaSteps))
                .param("exitsVpc", "true"));
    }

    @Then("effective egress RPS becomes {int} req/s")
    public void verifyEffectiveEgressRps(Integer expectedEgressRps) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveEgressRps").value(expectedEgressRps));
    }

    @And("the additional egress cost is computed against {int} req/s")
    public void verifyEgressCostComputedAgainstRps(Integer computationRps) throws Exception {
        response.andExpect(jsonPath("$.egressComputationRps").value(computationRps));
    }

    // FIX: POST → GET
    @Given("base RPS of {int} and {int} SAGA steps exiting the VPC")
    public void setBaseRpsAndSagaOutline(Integer rps, Integer steps) throws Exception {
        this.baseRps = rps;
        this.sagaSteps = steps;
        this.exitsVpc = true;
        response = mockMvc.perform(get("/api/session/tactics")
                .param("tactic", "saga")
                .param("baseRps", String.valueOf(rps))
                .param("sagaSteps", String.valueOf(steps))
                .param("exitsVpc", "true"));
    }

    @Then("billable egress RPS becomes {int} and monthly cost increase is approximately {string}")
    public void verifyBillableEgressRpsAndCost(Integer expectedEgressRps, String expectedCost) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveEgressRps").value(expectedEgressRps))
                .andExpect(jsonPath("$.monthlyCostDelta").value(expectedCost));
    }
}