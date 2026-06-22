@ArchitectureCalculator @ChooseQualityAttributeFeature
Feature: Software Architectural Characteristic / Driver / Quality Attribute Selection
  As a Software Architect
  So that I can design a MACH Architecture that meets business-context-driven non-functional requirements
  I want to select appropriate quality attributes

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @Resiliency
  Scenario: Selecting Resiliency as the primary architectural driver
    Given the system must withstand failures and recover quickly to maintain essential operations
    When I select "Resiliency" as the primary architectural driver
    Then the system should list applicable "resiliency tactics"
    And suggest potential "architectural patterns" supporting "resiliency"

  @UI @Reliability
  Scenario: Selecting Reliability as the primary architectural driver
    Given the system must operate correctly over time without failures
    When I select "Reliability" as the primary quality attribute
    Then the system should list applicable "reliability tactics"
    And suggest potential "architectural patterns" supporting "reliability"

  @UI @Security
  Scenario: Selecting Security as an architectural driver
    Given the system processes sensitive data
    When I select "Security" as the primary architectural driver
    Then the system should list applicable "security tactics"
    And suggest potential "architectural patterns" supporting "security"

  @API @DataDriven
  Scenario Outline: Quality attribute and architectural tactics mapping
    Given a project with "<business_requirement>"
    When I select "<architectural_characteristic>" as an architectural driver
    Then the REST service response should suggest "<recommended_tactics>" as potential implementation options

    Examples:
      | business_requirement              | architectural_characteristic | recommended_tactics                                                     |
      | Failure recovery needs            | Resiliency                   | Timeout, Circuit Breaker, Retry                                         |
      | Correct operation over time needs | Reliability                  | Client-side Load Balancing, Server-side Load Balancing                  |
      | Data protection needs             | Security                     | TLS (One-way), mTLS (Mutual TLS), OAuth 2.0 + JWT                       |