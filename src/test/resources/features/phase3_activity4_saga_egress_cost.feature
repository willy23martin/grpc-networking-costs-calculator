# Language: en
@ArchitectureCalculator @Phase3 @Activity4 @SAGAPattern
Feature: SAGA Pattern and Inter-Service Egress Cost
  As a Software Architect
  So that I can understand the networking cost of distributed transactions
  I want to configure the SAGA pattern and see the inter-service egress impact

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @SAGA @IntraVPC
  Scenario: SAGA steps within the VPC incur no egress charge
    Given all SAGA steps call services within the same AWS VPC
    When I configure 3 SAGA steps and leave "exits VPC" unchecked
    Then the live delta shows $0 additional egress
    And the tactic appears as "SAGA (3 steps, intra-VPC) — $0.00 same-AZ egress"

  @UI @SAGA @ExtraVPC
  Scenario: SAGA steps exiting the VPC multiply billable egress RPS
    Given the base RPS is 1000 req/s and 3 SAGA steps exit the VPC
    When the "SAGA steps call services outside AWS VPC" checkbox is enabled
    Then effective egress RPS becomes 3000 req/s
    And the additional egress cost is computed against 3000 req/s

  @UI @SAGA @CostDelta
  Scenario Outline: SAGA step count and egress cost
    Given base RPS of <rps> and <steps> SAGA steps exiting the VPC
    Then billable egress RPS becomes <egress_rps> and monthly cost increase is approximately "<cost_delta>"

    Examples:
      | rps  | steps | egress_rps | cost_delta  |
      | 1000 | 2     | 2000       | +$20.74     |
      | 1000 | 3     | 3000       | +$41.47     |
      | 5000 | 2     | 10000      | +$103.68    |
