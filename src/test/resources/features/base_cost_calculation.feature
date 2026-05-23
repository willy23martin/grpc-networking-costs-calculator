# Language: en
@ArchitectureCalculator @Activity4
Feature: Base Cost Calculation Using Cloud Pricing Calculators
  As a Software Architect
  So that I can provide accurate budget forecasts to stakeholders
  I want to calculate base costs of my architectural decisions

  Background:
    Given the calculator workspace is initialized for cloud financial modeling
    And the architect has verified that only AWS components are available in the view

  @UI @BaseCostCalculation
  Scenario Outline: Base cost calculation for different gRPC-based MACH architecture configurations
    Given I have selected "<cloud_service>" to implement "<tactic>"
    And my provider is "<cloud_provider>"
    When I have specified "<usage_parameter>" for the service
    Then I calculate the base costs
    And the system should show "<hourly_cost>" estimates
    And highlight "<cost_factors>" as primary cost drivers

    Examples:
      | cloud_provider | cloud_service               | tactic                          | usage_parameter               | hourly_cost | cost_factors                                    |
      | AWS            | Elastic Load Balancer (ELB) | Server-side Load Balancing      | 10,000 requests/sec           | $0.11       | Request volume, Data processed, Instance hours  |
      | AWS            | App Mesh                    | Client-side Load Balancing      | 50 microservices, 1TB traffic | $0.045      | Number of Envoy proxies, Data transfer          |
      | AWS            | API Gateway                 | Circuit Breaker / gRPC Security | 5M requests per month         | $0.072      | Request volume, Data size                       |
      | AWS            | Certificate Manager         | TLS Security                    | 100 certificates              | $0.021      | Certificate count, Renewals                     |

  @API @PricingService
  Scenario Outline: Fetching raw reference pricing matrices for AWS estimations via REST API
    Given a pricing query for component "<cloud_service>" under provider "<cloud_provider>"
    When the architectural calculator executes cost analysis for "<usage_parameter>"
    Then the JSON response must compute a corresponding baseline estimate of "<hourly_cost>"
    And return drivers matching "<cost_factors>"

    Examples:
      | cloud_provider | cloud_service               | usage_parameter       | hourly_cost | cost_factors                                   |
      | AWS            | Elastic Load Balancer (ELB) | 10,000 requests/sec   | $0.11       | Request volume, Data processed, Instance hours |
      | AWS            | API Gateway                 | 5M requests per month | $0.072      | Request volume, Data size                      |