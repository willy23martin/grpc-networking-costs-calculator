package com.calculator.features;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OAuthJwtTacticRpsCostCalculationSteps {

    private int baseRps;
    private int adjustedRps;

    @Given("a basis requests per second of {int} rps")
    public void setBaseRps(int rps) {
        this.baseRps = rps;
    }

    @When("the software architect selects OAuth plus JWT token as an architectural tactic with Remote Instrospection")
    public void selectOAuthJwtTactic() {
        // Remote introspection doubles RPS overhead; adjust accordingly
        this.adjustedRps = baseRps * 2;
    }

    @Then("the requests per second should increase up to {int} rps")
    public void verifyAdjustedRps(int expectedRps) {
        assertEquals(expectedRps, adjustedRps);
    }
}