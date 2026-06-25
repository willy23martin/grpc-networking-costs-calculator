@Activity3 @CostEfficiencyCalculator @MapCloudServicesToImplementTheChosenTactics
Feature: Cloud Service Mapping
  As a Software Architect
  So that I can implement a cost-aware architecture using concrete cloud provider offerings
  I want to map selected tactics to specific cloud services

  Background:
    Given the architect is authenticated on the TCO Calculator application
    And the architect is configuring tactics in the Cloud Tactics & Patterns panel

  @UI @CloudMapping @AWS
  Scenario Outline: Mapping architectural patterns and tactics to AWS cloud infrastructure in the UI
    Given I have selected pattern or tactic "<pattern_or_tactic>" for implementation that promotes "<architectural_characteristic>"
    When I specify "<cloud_provider>" as my deployment target
    Then the system should map the configuration to specific services "<specific_services>"

    Examples:
      | architectural_characteristic | pattern_or_tactic          | cloud_provider | specific_services                                                                                |
      | RELIABILITY                  | Server-side Load Balancing | AWS            | Elastic Load Balancer - ALB Layer 7                                                              |
      | SECURITY                     | TLS (One-way)              | AWS            | Amazon Inspector, Certificate Manager, AWS WAF, Amazon CloudWatch, AWS Audit Manager, AWS KMS    |
      | SECURITY                     | mTLS (Mutual TLS)          | AWS            | Amazon Inspector, Certificate Manager, AWS WAF, Amazon CloudWatch, AWS Audit Manager, AWS KMS    |
      | SECURITY                     | OAuth 2.0 + JWT            | AWS            | Amazon GuardDuty, AWS CloudTrail, Amazon Macie, Amazon CloudWatch, AWS Audit Manager             |