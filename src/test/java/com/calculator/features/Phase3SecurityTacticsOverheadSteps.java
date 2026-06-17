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
public class Phase3SecurityTacticsOverheadSteps {

    @Autowired
    private MockMvc mockMvc;

    private int protoResponseBytes;
    private String jwtValidationMode;
    private String activeSecurityTactic;
    private ResultActions response;

    @Given("the proto response message is {int} bytes")
    public void setProtoResponseSize(Integer bytes) {
        this.protoResponseBytes = bytes;
    }

    // FIX: POST /api/session/tactics → GET /api/session/tactics
    @When("I enable TLS {float} one-way")
    public void enableTls(Float version) throws Exception {
        response = mockMvc.perform(get("/api/session/tactics")
                .param("tactic", "tls")
                .param("tlsVersion", String.valueOf(version))
                .param("mode", "one-way")
                .param("protoResponseBytes", String.valueOf(protoResponseBytes)));
    }

    @Then("the effective response size increases by {int} bytes")
    public void verifyResponseSizeIncrease(Integer expectedOverheadBytes) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.byteOverhead").value(expectedOverheadBytes))
                .andExpect(jsonPath("$.effectiveResponseBytes")
                        .value(protoResponseBytes + expectedOverheadBytes));
    }

    @And("the AWS Certificate Manager ACM note confirms no additional certificate cost")
    public void verifyAcmNoCost() throws Exception {
        response.andExpect(jsonPath("$.certificateCost").value(0));
        mockMvc.perform(get("/calculator"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("AWS ACM")));
    }

    @Given("JWT validation mode is set to {string}")
    public void setJwtValidationMode(String mode) {
        this.jwtValidationMode = mode;
    }

    // FIX: POST → GET
    @When("I enable OAuth {float} plus JWT")
    public void enableOAuthJwt(Float oauthVersion) throws Exception {
        response = mockMvc.perform(get("/api/session/tactics")
                .param("tactic", "oauth-jwt")
                .param("oauthVersion", String.valueOf(oauthVersion))
                .param("jwtValidationMode", jwtValidationMode));
    }

    @Then("a {int}-byte JWT header is added to each request")
    public void verifyJwtHeaderOverhead(Integer jwtBytes) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.jwtHeaderBytes").value(jwtBytes));
    }

    @And("the cost delta shows $0 additional AWS egress because inbound traffic is free on AWS")
    public void verifyZeroEgressCostForInbound() throws Exception {
        response.andExpect(jsonPath("$.egressCostDelta").value(0));
    }

    @And("token acquisition calls per second are computed from RPS divided by TTL times clients")
    public void verifyTokenAcquisitionFormula() throws Exception {
        response.andExpect(jsonPath("$.tokenAcquisitionRps").exists());
    }

    // FIX: POST → GET
    @Given("the architect enables {string}")
    public void enableSecurityTactic(String tactic) throws Exception {
        this.activeSecurityTactic = tactic;
        response = mockMvc.perform(get("/api/session/tactics")
                .param("tactic", tactic));
    }

    @Then("it addresses {string} and adds {string} to each message")
    public void verifyOwaspAndOverhead(String owaspCategories, String overhead) throws Exception {
        response.andExpect(status().isOk())
                .andExpect(jsonPath("$.owaspCategories").value(owaspCategories))
                .andExpect(jsonPath("$.overheadProfile").value(overhead));
    }
}