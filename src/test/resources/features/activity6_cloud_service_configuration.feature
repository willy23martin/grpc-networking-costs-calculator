# language: en
@CostEfficiencyCalculator @Activity6 @FinOps
Feature: Cloud Service Configuration Modification
  As a Software Architect
  I want to modify cloud service configurations based on optimization strategies
  So that I can implement my design with optimal cost-efficiency

  @UI @API @Tuning
  Scenario Outline: Optimizing cloud service configurations for cost
    Given I have implemented "<cloud_service>" and a "<cloud_instance>"
    When I modify the configuration with "<finops_strategy>"
    Then the system should calculate "<monthly_savings>"

    Examples: User Story: Cloud Service Configuration Modification - Feature Examples
      | cloud_service    | cloud_instance | finops_strategy                | monthly_savings |
      | AWS EC2          | t3.medium      | Reserved Instance 1 year       | $10.93          |