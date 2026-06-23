@CostEfficiencyCalculator @Phase3 @Activity6 @LiveCostDelta
Feature: Live Cost Delta Validation
  As a Software Architect
  So that I can understand the cumulative financial impact of my tactical decisions before committing
  I want to see the real-time cost delta between the base configuration and the current tactic selection

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @LiveDelta @TacticBreakdown
  Scenario: Delta panel shows per-tactic cost contribution
    Given the architect has set a base RPS and selected one or more tactics
    When any tactic is toggled or a cloud infra value is changed
    Then the panel immediately updates with the base cost in USD per month
    And the panel shows tactics cost including networking plus cloud infra minus FinOps savings
    And the panel shows a per-tactic breakdown table with each tactic type and estimated monthly impact
    And the panel shows a delta row in red for cost increase or green for saving

  @UI @LiveDelta @PlaceholderSizes
  Scenario: Placeholder sizes are used until proto is uploaded
    Given no proto file has been uploaded yet
    When the live comparison renders
    Then the tool uses placeholder sizes of 200 bytes request and 1200 bytes response with a disclaimer
    And once the proto is submitted in Phase 2 exact backend-calculated sizes replace the placeholders
