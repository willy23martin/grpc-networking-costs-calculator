# Language: en
@ArchitectureCalculator @Activity5 @FinOps
Feature: Cost Optimization Strategy Definition
  As a Software Architect
  So that I can reduce operational expenses while maintaining quality
  I want to define FinOps cost optimization strategies

  Background:
    Given the architect is reviewing baseline costs on the TCO Calculator
    And the calculator view is validated to exclude non-AWS cloud platforms

  @UI @Optimization
  Scenario: Define reserved instance cost optimization strategy
    Given I have calculated base costs for "Container Service" at "$1000 monthly"
    When I define a "Reserved Instance" cost optimization strategy with "1-year commitment"
    Then the system should calculate potential savings of "35%"
    And show modified monthly costs with the strategy applied

  @UI @Optimization
  Scenario: Define auto-scaling cost optimization strategy
    Given I have calculated base costs for "Container Service" at "$1000 monthly"
    When I define an "Auto-scaling" strategy with "min 5, max 20 instances"
    Then the system should calculate potential savings of "22%"
    And show hourly cost variation based on load patterns

  @UI @API @TradeOffs
  Scenario Outline: FinOps strategy impact on reliability tactics implementation
    Given a reliability tactic "<reliability_tactic>" implementation with baseline cost "<base_monthly_cost>"
    When I apply the cost optimization strategy "<finops_strategy>"
    Then the system should predict a savings percentage of "<savings_percentage>" in cost reduction
    And indicate a reliability impact of "<reliability_impact>" on the original tactic effectiveness

    Examples:
      | reliability_tactic | base_monthly_cost | finops_strategy     | savings_percentage | reliability_impact                |
      | Server-side LB     | $500              | Reserved Instances  | 30%                | None                              |
      | Client-side LB     | $300              | Spot Instances      | 70%                | Medium increase in failure risk   |
      | Circuit Breaker    | $200              | Auto-scaling        | 25%                | Low impact on recovery time       |