# language: en
@CostEfficiencyCalculator @Activity7 @FinOps
Feature: Optimized Cost Calculation
  As a Software Architect
  I want to calculate costs after applying optimization strategies
  So that I can quantify financial impact and present cost savings

  @UI @API @Projections
  Scenario Outline: Cost impact for FinOps strategy across cloud service
    Given I have applied "<finops_strategy>" to services implementing "<cloud_service>"
    When I recalculate total costs across the architecture
    Then the system should show "<new_monthly_cost>" as the optimized cost
    And calculate "<annual_savings>"

    Examples: User Story: Optimized Cost Calculation - Feature Examples
      | finops_strategy | cloud_service | new_monthly_cost | annual_savings |
      | Spot Instances  | AWS EKS       | $36.50            | $36.50         |