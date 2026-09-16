@CostEfficiencyCalculator @Activity5 @FinOps
Feature: Cost Optimization Strategy Definition
  As a Software Architect
  So that I can reduce operational expenses while maintaining quality
  I want to apply FinOps cost optimization strategies

  @UI @API @TradeOffs
  Scenario Outline: FinOps strategy impact on cloud services implementation
    Given a cloud service "<cloud_service>" and a "<cloud_instance>" implementation with baseline cost "<base_monthly_cost>"
    When I apply the cost optimization strategy "<finops_strategy>"
    Then the system should predict a savings percentage of "<savings_percentage>" in cost reduction
    And indicate the impact of "<impact>" on the original tactic effectiveness

    Examples: User Story: Cost Optimization Strategy Definition - Feature Examples
      | cloud_service | cloud_instance | base_monthly_cost | finops_strategy           | savings_percentage | impact                                                                                                             |
      | AWS EC2       | t3.medium      | $30.37            | Reserved Instance 1 year  | 37%                | Reserved Instances are best for predictable, steady workloads like 24/7 production servers. |