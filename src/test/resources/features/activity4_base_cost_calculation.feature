# language: en
Feature: Base Cost Calculation
  As a Software Architect
  So that I can provide accurate budget forecasts to stakeholders
  I want to calculate base costs of my architectural decisions

  Scenario Outline: Base cost calculation for different gRPC-based MACH architecture configurations
    Given I have selected "<cloud_service>" to implement "<tactic>"
    And my base cost provider is "<cloud_provider>"
    When I trigger the base cost calculation
    Then the system should show "<hourly_cost>" estimates
    And highlight "<cost_factors>" as primary cost drivers

    Examples: User Story: Base Cost Calculation - Feature Examples
      | cloud_provider | cloud_service                       | tactic                          | hourly_cost | cost_factors                                                                                                                                                              |
      | AWS            | Elastic Load Balancer - ALB Layer 7 | Server-side Load Balancing      | $0.025     | $0.008 per GB Data Processed by the LoadBalancer $0.025 per LoadBalancer-hour (or partial hour) |