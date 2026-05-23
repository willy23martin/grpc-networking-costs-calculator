@ArchitectureCalculator @ChooseQualityAttributeFeature
Feature: Software Architectural Characteristic / Driver / Quality Attribute Selection
  As a Software Architect
  So that I can design a MACH Architecture that meets business-context-driven non-functional requirements
  I want to select appropriate quality attributes

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @Reliability
  Scenario: Selecting Reliability as the primary architectural driver
    Given the system must operate correctly over time without failures
    When I view or select "Reliability" tactics in the interface
    Then the system should list the following applicable reliability options:
      | Client-side Load Balancing |
      | Server-side Load Balancing |
      | Timeout-Deadline           |
      | Timeout-Cancellation       |
    And suggest potential architectural patterns supporting reliability

  @UI @Resiliency
  Scenario: Selecting Resiliency as the primary architectural driver
    Given the system must withstand failures and recover quickly to maintain essential operations
    When I view or select "Resiliency" tactics in the interface
    Then the system should list the following applicable resiliency options:
      | Retry pattern             |
      | Circuit Breaker pattern   |
      | gRPC Health Probe         |
      | Retry-Interceptor         |
    And suggest potential architectural patterns supporting resiliency

  @UI @Security
  Scenario: Selecting Security as an architectural driver
    Given the system processes sensitive data
    When I view or select "Security" tactics in the interface
    Then the system should list the following applicable security options:
      | TLS Handshake             |
      | Certificates              |
      | gRPC TLS credentials      |
    And suggest potential architectural patterns supporting security

  @API @DataDriven
  Scenario Outline: Quality attribute and architectural tactics mapping
    Given a project with business requirement "<business_requirement>"
    When the backend processes a request for quality attribute "<quality_attr>"
    Then the REST service response should suggest "<recommended_tactics>" as potential implementation options

    Examples:
      | business_requirement              | quality_attr | recommended_tactics                                                                           |
      | Failure recovery needs            | Resiliency   | Retry pattern, Circuit Breaker pattern, gRPC Health Probe, Retry-Interceptor                  |
      | Correct operation over time needs | Reliability  | Timeout-Deadline, Timeout-Cancellation, Client Side Load Balancing, Server-side Load Balancing |
      | Data protection needs             | Security     | TLS Handshake, Certificates, gRPC TLS credentials                                             |