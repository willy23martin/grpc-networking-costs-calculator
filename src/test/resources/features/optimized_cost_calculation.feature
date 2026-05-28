# Language: en
@ArchitectureCalculator @Activity7 @FinOps @CostCalculation
Feature: Optimized Cost Calculation
  As a Software Architect
  I want to calculate costs after applying optimization strategies
  So that I can quantify financial impact and present cost savings

  Background:
    Given the architect is assessing optimized reports on the TCO Calculator
    And the calculation baseline strictly targets AWS infrastructure services

  @UI @Optimization
  Scenario: Calculate costs after applying reserved instance strategy
    Given I have modified "Load Balancer" configuration with "Reserved Instance" strategy
    When I recalculate the service costs
    Then the system should show reduced monthly costs of "$520"
    And calculate the total savings over the commitment period as "$3360"

  @UI @Resiliency
  Scenario: Calculate costs after optimizing circuit breaker implementation
    Given I have modified "API Gateway" with optimized circuit breaker parameters
    When I recalculate the service costs with actual usage metrics
    Then the system should show reduced error rates by "45%"
    And calculate monthly cost savings of "$120" from reduced retry operations

  @UI @API @ReportSummary
  Scenario Outline: Cost impact for FinOps strategy across reliability patterns
    Given I have applied "<finops_strategy>" to services implementing "<reliability_pattern>"
    When I recalculate total costs across the architecture
    Then the system should show "<new_monthly_cost>" as the optimized cost
    And calculate "<annual_savings>" while maintaining "<availability_target>"

    Examples:
      | finops_strategy           | reliability_pattern | new_monthly_cost | annual_savings | availability_target |
      | Reserved Instances        | Load Balancing      | $3250            | $11400         | 99.99%              |
      | Auto-scaling              | Circuit Breaker     | $2800            | $9600          | 99.95%              |
      | Multi-region optimization | Timeout+Retry       | $4100            | $7200          | 99.999%             |