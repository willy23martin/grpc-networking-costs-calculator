# Language: en
@ArchitectureCalculator @Phase3 @Activity2 @ResiliencyPatterns
Feature: Resiliency Pattern Configuration and RPS Impact
  As a Software Architect
  So that I can quantify the networking cost of fault-tolerance mechanisms
  I want to configure resiliency patterns and observe their RPS impact in real time

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @Retry @RPS
  Scenario: Retry pattern increases effective RPS
    Given the base RPS is 1000 req/s
    When I enable the Retry tactic with a 5% error rate
    Then the effective RPS increases by 50 req/s to 1050 req/s
    And the live cost delta panel shows the additional egress cost

  @UI @CircuitBreaker @RPS
  Scenario: Circuit Breaker has no RPS impact
    Given any base RPS
    When I enable the Circuit Breaker tactic
    Then the effective RPS is unchanged
    And the tactic appears in the "Informational" row of the tactics breakdown

  @UI @Retry @CostDelta
  Scenario Outline: Retry error rate drives egress cost increase
    Given the base RPS is <base_rps> req/s
    When Retry is enabled with "<error_rate>"
    Then effective RPS becomes <effective_rps>
    And monthly egress cost increase is approximately "<cost_delta>"

    Examples:
      | base_rps | error_rate | effective_rps | cost_delta |
      | 1000     | 5%         | 1050          | +$0.12     |
      | 1000     | 10%        | 1100          | +$0.24     |
      | 5000     | 5%         | 5250          | +$0.60     |