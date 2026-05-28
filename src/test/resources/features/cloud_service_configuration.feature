# Language: en
@ArchitectureCalculator @Activity6 @FinOps @Configuration
Feature: Cloud Service Configuration Modification
  As a Software Architect
  I want to modify cloud service configurations based on optimization strategies
  So that I can implement my design with optimal cost-efficiency

  Background:
    Given the architect is managing configurations on the TCO Calculator
    And the configuration dashboard is verified to contain only AWS cloud environments

  @UI @Configuration
  Scenario: Apply reserved instance configuration to reliability services
    Given I have selected "Reserved Instance" strategy for "Load Balancer" service
    When I modify the service configuration with "1-year commitment" option
    Then the system should update the ServiceFinOpsConfiguration with commitment type
    And generate implementation instructions for the operations team

  @UI @Resiliency
  Scenario: Apply circuit breaker configuration with timeout optimizations
    Given I have selected "Circuit Breaker" pattern with "Timeout Pattern"
    When I configure timeout values of "2000ms" and failure threshold of "5"
    Then the system should update the ServiceFinOpsConfiguration with these parameters
    And generate implementation code examples for the gRPC services

  @UI @Security @Optimization
  Scenario Outline: Optimizing security tactic configurations for cost
    Given I have implemented "<security_tactic>" using "<cloud_service>"
    When I modify the configuration with "<optimization_parameter>"
    Then the system should update the ServiceFinOpsConfiguration
    And estimate "<monthly_savings>" while maintaining "<security_level>"

    Examples:
      | security_tactic | cloud_service       | optimization_parameter      | monthly_savings | security_level |
      | TLS handshake   | Certificate Manager | 1-year certificate validity | $25             | High           |
      | gRPC TLS        | API Gateway         | Connection pooling enabled  | $60             | High           |