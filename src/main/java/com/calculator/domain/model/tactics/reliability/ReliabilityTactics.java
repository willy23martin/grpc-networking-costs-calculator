package com.calculator.domain.model.tactics.reliability;

public record ReliabilityTactics(
        boolean reliabilityClientSideLoadBalancerTactic,
        boolean reliabilityServerSideLoadBalancerTactic
) {
}
