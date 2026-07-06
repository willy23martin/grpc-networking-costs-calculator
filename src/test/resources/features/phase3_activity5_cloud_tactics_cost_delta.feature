@CostEfficiencyCalculator @Phase3 @Activity5 @CloudTactics
Feature: Cloud Infrastructure Tactics and Live Cost Delta
  As a Software Architect
  So that I can include cloud infrastructure costs beyond network egress in my TCO model
  I want to configure AWS cloud tactics and observe their costs immediately in the live delta panel

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @ALB @PricingAPI
  Scenario: ALB cost is computed from live AWS Pricing API data
    Given the architect opens the "ALB & Load Balancer Costs" accordion
    When the section loads the tool calls GET /api/aws/alb-pricing
    Then the live per-hour and per-LCU rates are displayed
    And when the architect enters the number of ALBs and LCUs the monthly cost is computed using the fixed plus LCU formula
    And the result is stored in application state and immediately reflected in the live cost delta

  @UI @EC2 @ReplicaFormula
  Scenario: Replica sizing formula drives EC2 compute cost
    Given the architect selects an EC2 instance type
    Then the minimum replica count is computed from RPS request rate capacity and throughput constraints
    And monthly EC2 cost is shown as replica count times price per hour times 730

  @UI @FinOps @ReservedInstance
  Scenario: FinOps Reserved Instance discount reduces total cloud spend
    Given the architect has entered an on-demand EC2 spend
    When Standard RI 1-yr is selected
    Then the system calls GET /api/finops/ri-prices for the instance type to fetch live AWS discount percentages
    And the savings are subtracted from the total cloud infrastructure cost in both the live delta panel and the Phase 4 TCO breakdown

  @API @CloudTactics @Mapping
  Scenario Outline: Cloud tactics and their AWS Pricing API endpoints
    Given the architect configures the "<cloud_tactic>" cloud tactic
    Then the system targets the "<aws_target_service>" AWS service

    Examples:
      | cloud_tactic          | aws_target_service                |
      | Scalability (EC2)     | /api/aws/ec2-instances            |
      | ALB                   | /api/aws/alb-pricing              |
      | Database / Backup     | /api/aws/database-backup-pricing  |
      | Cloud Security        | /api/aws/security-services        |
      | Caching (ElastiCache) | /api/aws/caching-pricing          |
      | FinOps RI/SP          | /api/aws/cost-optimisation        |
