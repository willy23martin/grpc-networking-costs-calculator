@ArchitectureCalculator @ChooseTacticsToPromoteQualityAttributes
Feature: Architecture Tactics and Patterns Selection
  As a Software Architect
  So that I can implement technical strategies addressing business needs
  I want to select specific tactics or patterns for my chosen quality attributes

  Background:
    Given the architect is configured with a base service on the TCO Calculator
    And the architect is navigating Phase 3 "Architecture Tactics & Patterns"

  @UI @API @TradeOffs
  Scenario Outline: Tactic selection trade-offs
    Given I have selected "<architectural_characteristic>" as the primary focus.
    When I choose "<specific_tactic>" as the implementation approach.
    Then the system should identify "<impacted_attr>" with "<impact_type>"

    Examples:
      | architectural_characteristic | specific_tactic            | impacted_attr | impact_type |
      | Resiliency                   | Timeout                    | Affordability | ORTHOGONAL  |
      | Resiliency                   | Retry                      | Affordability | INHIBITS     |
      | Resiliency                   | Circuit Breaker            | Affordability | ORTHOGONAL  |
      | Reliability                  | Client-side Load Balancing | Affordability | ORTHOGONAL  |
      | Reliability                  | Server-side Load Balancing | Affordability | INHIBITS     |
      | Security                     | TLS (One-way)              | Affordability | INHIBITS     |
      | Security                     | mTLS (Mutual TLS)          | Affordability | INHIBITS     |
      | Security                     | OAuth + JWT                | Affordability | INHIBITS     |