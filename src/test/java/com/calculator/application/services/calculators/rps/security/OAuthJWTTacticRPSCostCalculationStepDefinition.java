package com.calculator.application.services.calculators.rps.security;

import com.calculator.application.services.calculators.rps.security.jwt.RPSJWTCostCalculator;
import com.calculator.domain.dto.tactics.security.oauth.jwt.JWTTactic;
import com.calculator.domain.model.architecture.ArchitecturalTactic;
import com.calculator.domain.model.architecture.tactics.security.OAuthTokenValidationModes;
import com.calculator.domain.model.architecture.tactics.security.SecurityTactics;
import com.calculator.domain.model.cost.*;
import com.calculator.domain.model.cost.NetworkingCost;
import com.calculator.domain.model.cost.networking.NetworkingCostCriteria;
import com.calculator.domain.model.quality.ArchitecturalCharacteristic;
import com.calculator.domain.model.quality.ArchitecturalCharacteristics;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

public class OAuthJWTTacticRPSCostCalculationStepDefinition {

    private long requestsPerSecond;

    private ArchitecturalCharacteristic architecturalCharacteristic;
    private ArchitecturalTactic architecturalTactic;
    private CostFactor costFactor;

    @Autowired
    private RPSJWTCostCalculator rpsjwtCostCalculator;

    @Given("a basis requests per second of {long} rps")
    public void a_basis_requests_per_second_of_rps(Long requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    @When("the software architect selects OAuth plus JWT token as an architectural tactic with Remote Instrospection")
    public void the_software_architect_selects_o_auth_plus_jwt_token_as_an_architectural_tactic_with_remote_instrospection() {
        this.architecturalCharacteristic = ArchitecturalCharacteristic
                .builder()
                .name(ArchitecturalCharacteristics.SECURITY.name())
                .build();

        this.costFactor = NetworkingCost
                .builder()
                .criteria(NetworkingCostCriteria.REQUESTS_PER_SECOND.name())
                .value(2)
                .operation(CostOperations.MULTIPLIER.name())
                .units(CostUnits.RPS.name())
                .build();

        this.architecturalTactic = ArchitecturalTactic
                .builder()
                .architecturalCharacteristic(architecturalCharacteristic)
                .name(SecurityTactics.OAUTH_PLUS_JWT_PLUS_REMOTE_INTROSPECTION.name())
                .costFactors(Map.of(
                        CostFactors.RPS.name(), costFactor
                ))
                .build();
    }

    @Then("the requests per second should increase up to {long} rps")
    public void the_requests_per_second_should_increase_up_to_rps(Long effectiveRequestsPerSecond) {
        NetworkingCost networkingCost = (NetworkingCost) this.architecturalTactic.getCostFactors().get(CostFactors.RPS.name());
        Assertions.assertThat(
                rpsjwtCostCalculator.extraRequestsPerSecondFromRemoteTokenIntrospection(this.requestsPerSecond,
                        JWTTactic.builder()
                                .oauthJwtEnabled(true)
                                .tokenValidationMode(OAuthTokenValidationModes.REMOTE_INTROSPECTION)
                                .build()
                )
        ).isEqualTo(effectiveRequestsPerSecond);
    }
}
