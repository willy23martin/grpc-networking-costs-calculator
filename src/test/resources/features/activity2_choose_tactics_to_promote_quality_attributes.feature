@Activity2 @CostEfficiencyCalculator @ChooseTacticsToPromoteQualityAttributes
Feature: Architecture Tactics and Patterns Selection
  As a Software Architect
  So that I can implement technical strategies addressing business needs
  I want to select specific tactics or patterns for my chosen quality attributes

  @ArchitectureAlignment @DirectImpact
  Scenario Outline: Tactic selection to promote quality attributes
    Given I have selected "<architectural_characteristic>" as the primary focus
    When I choose "<specific_tactic>" as the implementation approach
    Then the system should identify "<impacted_attr>" with "<impact_type>"

    Examples:
      | architectural_characteristic | specific_tactic            | impacted_attr | impact_type |
      | Resiliency                   | Timeout                    | RESILIENCY    | PROMOTES    |
      | Resiliency                   | Retry                      | RESILIENCY    | PROMOTES    |
      | Resiliency                   | Circuit Breaker            | RESILIENCY    | PROMOTES    |
      | Reliability                  | Client-side Load Balancing | RELIABILITY   | PROMOTES    |
      | Reliability                  | Server-side Load Balancing | RELIABILITY   | PROMOTES    |
      | Security                     | TLS (One-way)              | SECURITY      | PROMOTES    |
      | Security                     | mTLS (Mutual TLS)          | SECURITY      | PROMOTES    |
      | Security                     | OAuth 2.0 + JWT            | SECURITY      | PROMOTES    |
      | Resiliency                   | Timeout                    | AFFORDABILITY | ORTHOGONAL  |
      | Resiliency                   | Retry                      | AFFORDABILITY | INHIBITS    |
      | Resiliency                   | Circuit Breaker            | AFFORDABILITY | ORTHOGONAL  |
      | Reliability                  | Client-side Load Balancing | AFFORDABILITY | ORTHOGONAL  |
      | Reliability                  | Server-side Load Balancing | AFFORDABILITY | INHIBITS    |
      | Security                     | TLS (One-way)              | AFFORDABILITY | INHIBITS    |
      | Security                     | mTLS (Mutual TLS)          | AFFORDABILITY | INHIBITS    |
      | Security                     | OAuth 2.0 + JWT            | AFFORDABILITY | INHIBITS    |

