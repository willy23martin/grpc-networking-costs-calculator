package com.calculator.domain.dto.tactics.reliability;

public record ReliabilityTactics(
        boolean reliabilityClientSideLoadBalancerTactic,
        boolean reliabilityServerSideLoadBalancerTactic
) {
    public static ReliabilityTactics empty() {
        return new ReliabilityTactics(false, false);
    }
}