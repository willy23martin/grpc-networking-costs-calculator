Feature: Requests per second increments with OAuth JWT with Remote Instrospection
  Scenario: software architect selects OAuth plus JWT token as an architectural tactic
    Given a basis requests per second of 1000 rps
    When the software architect selects OAuth plus JWT token as an architectural tactic with Remote Instrospection
    Then the requests per second should increase up to 2000 rps