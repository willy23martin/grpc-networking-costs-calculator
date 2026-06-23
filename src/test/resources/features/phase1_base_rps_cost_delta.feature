@CostEfficiencyCalculator @Phase1 @ServiceIdentity
Feature: Base RPS drives all downstream cost estimates
  As a Software Architect
  So that I can understand the financial baseline of my service workload
  I want to enter a base RPS and see how tactic-driven load increases affect egress costs

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @RPS @CostDelta
  Scenario Outline: Base RPS drives egress cost delta when a tactic increases effective load
    Given a service with <base_rps> requests per second
    When the architect selects a tactic that increases effective RPS by "<rps_delta>"
    Then the live cost delta panel shows an egress cost increase of approximately "<expected_delta_usd>" per month

    Examples:
      | base_rps | rps_delta                       | expected_delta_usd |
      | 1000     | +50 RPS (5% Retry Profile)      | $0.12 / mo         |
      | 5000     | +250 RPS (5% Retry Profile)     | $0.60 / mo         |
      | 10000    | +10000 RPS (SAGA Cascading)     | $43.20 / mo        |
