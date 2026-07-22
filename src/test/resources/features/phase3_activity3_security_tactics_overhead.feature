@CostEfficiencyCalculator @Phase3 @Activity3 @SecurityTactics
Feature: Security Tactic Configuration and Byte Overhead
  As a Software Architect
  So that I can account for the byte overhead of encryption and authentication in my TCO
  I want to enable security tactics and observe their impact on message sizes and egress costs

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @Security @OWASPMapping
  Scenario Outline: Security tactics OWASP mapping
    Given the architect enables "<security_tactic>"
    Then it addresses owasp category "<owasp_categories>" and adds "<overhead>" to each message

    Examples:
      | security_tactic      | owasp_categories                                        | overhead                  |
      | TLS (One-way)        | Cryptographic Failures                                  | 30 B                      |
      | mTLS (Mutual TLS)    | Broken Access Control                                   | 5 TLS messages            |
      | OAuth 2.0 + JWT      | Identification and Authentication Failures              | 650 B                     |
      | Basic Authentication | Identification and Authentication Failures              | 0                         |
