# Language: en
@ArchitectureCalculator @Phase3 @Activity3 @SecurityTactics
Feature: Security Tactic Configuration and Byte Overhead
  As a Software Architect
  So that I can account for the byte overhead of encryption and authentication in my TCO
  I want to enable security tactics and observe their impact on message sizes and egress costs

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @TLS @ByteOverhead
  Scenario: TLS 1.3 adds per-frame byte overhead
    Given the proto response message is 1200 bytes
    When I enable TLS 1.3 one-way
    Then the effective response size increases by 29 bytes
    And the AWS Certificate Manager ACM note confirms no additional certificate cost

  @UI @OAuth @JWT @ByteOverhead
  Scenario: OAuth 2.0 plus JWT adds request-side header overhead at no AWS egress charge
    Given JWT validation mode is set to "Local"
    When I enable OAuth 2.0 plus JWT
    Then a 650-byte JWT header is added to each request
    And the cost delta shows $0 additional AWS egress because inbound traffic is free on AWS
    And token acquisition calls per second are computed from RPS divided by TTL times clients

  @UI @Security @OWASPMapping
  Scenario Outline: Security tactics OWASP mapping
    Given the architect enables "<security_tactic>"
    Then it addresses "<owasp_categories>" and adds "<overhead>" to each message

    Examples:
      | security_tactic       | owasp_categories                  | overhead                  |
      | TLS 1.3 (one-way)    | Cryptographic Failures            | Fixed Frame Padding        |
      | mTLS (Mutual TLS)    | Identification/Auth Errors        | Handshake Message Load     |
      | OAuth 2.0 + JWT      | Broken Access Control             | Variable Header Size       |
      | Basic Authentication | Cryptographic / Auth Risks        | Fixed Payload Bytes        |
