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
    And the cost is calculated using "<cost_type>" pricing model

    Examples:
      | cloud_tactic           | aws_target_service                    | cost_type                  |
      | Availability SLA       | Multi-AZ Deployment Model             | Architecture Multiplier    |
      | Scalability (EC2)      | Compute Cloud (EC2 Instances)         | Instance-Hour Formula      |
      | ALB                    | Elastic Load Balancing (ALB)          | Fixed + LCU Metrics        |
      | Database / Backup      | Relational Database Service (RDS)     | Storage + Replica Rates    |
      | Cloud Security         | WAF / Shield / GuardDuty              | Usage-Based Tiers          |
      | Caching (ElastiCache)  | Redis / Memcached Nodes               | Node-Hour Base             |
      | Containerized Env.     | Elastic Kubernetes Service (EKS)      | Cluster + Compute          |
      | FinOps RI/SP           | Reserved Instances / Savings Plans    | Committed Spend Discount   |
