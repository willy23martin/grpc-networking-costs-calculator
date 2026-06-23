@CostEfficiencyCalculator @ChooseQualityAttributeFeature
Feature: Software Architectural Characteristic / Driver / Quality Attribute Selection
  As a Software Architect
  So that I can design a MACH Architecture that meets business-context-driven non-functional requirements
  I want to select appropriate quality attributes

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @API
  Scenario Outline: Quality attribute and architectural tactics mapping
    Given a project with "<business_requirement>"
    When I select "<architectural_characteristic>" as an architectural driver
    Then the system should suggest "<recommended_tactics>" as potential implementation options

    Examples:
      | business_requirement              | architectural_characteristic | recommended_tactics                                                     |
      | Failure recovery needs            | Resiliency                   | Timeout, Circuit Breaker, Retry                                         |
      | Correct operation over time needs | Reliability                  | Client-side Load Balancing, Server-side Load Balancing                  |
      | Data protection needs             | Security                     | TLS (One-way), mTLS (Mutual TLS), OAuth 2.0 + JWT                       |