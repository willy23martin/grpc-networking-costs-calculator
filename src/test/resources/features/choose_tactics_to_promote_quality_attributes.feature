# Language: en
@ArchitectureCalculator @ChooseTacticsToPromoteQualityAtrributes
Feature: Architectural Tactics and Patterns Selection Trade-offs
  As a Software Architect
  So that I can implement technical strategies addressing business needs
  I want to select specific tactics or patterns for my chosen quality attributes and evaluate trade-offs

  Background:
    Given the architect is configured with a base service on the TCO Calculator
    And the architect is navigating Phase 3 "Architecture Tactics & Patterns"

  @UI @TradeOffs
  Scenario Outline: Tactic selection trade-offs evaluation in the UI
    Given I have selected quality attribute focus "<quality_attr>"
    When I choose the tactic "<specific_tactic>" as implementation approach
    Then the interface should display that it impacts "<impacted_attr>" with type "<impact_type>"
    And the system should suggest "<mitig_measures>" as mitigating measures

    Examples:
      | quality_attr | specific_tactic            | impacted_attr   | impact_type | mitig_measures                                                |
      | Resiliency   | Retry pattern              | Performance     | INHIBITS    | Limit retry attempts                                          |
      | Resiliency   | Retry pattern              | Availability    | PROMOTES    | Use retries only for idempotent operations                    |
      | Resiliency   | Circuit Breaker            | Performance     | INHIBITS    | Optimize thresholds, avoid excessive opening                  |
      | Resiliency   | Circuit Breaker            | Availability    | PROMOTES    | Tune open/close thresholds                                    |
      | Resiliency   | gRPC Health Probe          | Performance     | INHIBITS    | Adjust probe frequency                                        |
      | Resiliency   | gRPC Health Probe          | Maintainability | PROMOTES    | Integrate with monitoring tools                               |
      | Resiliency   | Retry-Interceptor          | Performance     | INHIBITS    | Set maximum retries, monitor error rates                     |
      | Resiliency   | Retry-Interceptor          | Availability    | PROMOTES    | Use back-off                                                  |
      | Reliability  | Circuit Breaker            | Performance     | INHIBITS    | Optimize threshold configuration                              |
      | Reliability  | Circuit Breaker            | Availability    | PROMOTES    | Prevent cascading failures                                    |
      | Reliability  | Client-side Load Balancing | Maintainability | INHIBITS    | Document client logic                                         |
      | Reliability  | Client-side Load Balancing | Scalability     | PROMOTES    | Use auto-scaling with service discovery                       |
      | Reliability  | Server-side Load Balancing | Modifiability   | INHIBITS    | Decouple configuration from code                              |
      | Reliability  | Server-side Load Balancing | Scalability     | PROMOTES    | Scale backend instances                                       |
      | Security     | TLS handshake              | Performance     | INHIBITS    | Session resumption, connection pooling                        |
      | Security     | TLS handshake              | Confidentiality | PROMOTES    | Enforce strong cipher suites                                  |
      | Security     | Certificate generation     | Maintainability | INHIBITS    | Automate certificate renewal                                  |
      | Security     | Certificate generation     | Confidentiality | PROMOTES    | Use automated, secure certificate management                  |
      | Security     | gRPC TLS credentials       | Performance     | INHIBITS    | Use hardware acceleration for crypto                          |
      | Security     | gRPC TLS credentials       | Confidentiality | PROMOTES    | Enforce strong cipher suites                                  |

  @API @REST
  Scenario Outline: Fetching tactic trade-offs matrix via REST service
    Given a backend client requests trade-offs for attribute "<quality_attr>" and tactic "<specific_tactic>"
    When the REST endpoint returns the trade-off evaluation matrix
    Then the response JSON payload must indicate impact on "<impacted_attr>" is "<impact_type>"
    And the recommended mitigation strategy must match "<mitig_measures>"

    Examples:
      | quality_attr | specific_tactic            | impacted_attr   | impact_type | mitig_measures                                                |
      | Resiliency   | Retry pattern              | Performance     | INHIBITS    | Limit retry attempts                                          |
      | Resiliency   | Circuit Breaker            | Availability    | PROMOTES    | Tune open/close thresholds                                    |
      | Reliability  | Client-side Load Balancing | Scalability     | PROMOTES    | Use auto-scaling with service discovery                       |
      | Security     | TLS handshake              | Confidentiality | PROMOTES    | Enforce strong cipher suites                                  |