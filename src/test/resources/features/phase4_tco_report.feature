@CostEfficiencyCalculator @Phase4 @TCOReport
Feature: TCO Report Generation and Detailed Breakdown
  As a Software Architect
  So that I can present a financially grounded architecture decision to business stakeholders
  I want to generate a full TCO report backed by backend-calculated proto byte sizes and live AWS pricing

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @TCO @ProtoSubmission
  Scenario: Submitting the proto file triggers backend cost calculation
    Given a BUC has been selected and a proto file is loaded
    When the architect clicks Calculate TCO in Phase 3
    Then the tool calls POST api-session-tactics and POST calculateTCO
    And the interface automatically navigates to Phase 4 to render the full report

  @UI @TCO @ReportRendering
  Scenario: Phase 4 report renders the complete cost breakdown
    Given the backend has returned a valid TCO response
    When Phase 4 loads
    Then the report displays a Service Identity Banner with metadata and active tactics
    And the report displays an RPS Adjustment Banner showing load shifts
    And the report displays a Security Overhead Banner showing RFC byte additions
    And the report displays Tactics Summary Tables
    And the page renders a Proto Sizes Table with volumes throughput and AWS egress charges
    And the page renders a complete TCO Breakdown Table with networking cloud infra FinOps metrics and DR/BC status
    And the page renders Unit Economics breakdown ROI Analysis panel and FinOps Architecture Notes

  @UI @TCO @Portfolio
  Scenario: The service can be saved to the portfolio
    Given Phase 4 displays a non-zero TCO
    When the architect clicks Add to Portfolio
    Then the complete service entry profile is persisted to browser localStorage
    And a toast notification confirms the save making the portfolio available across page reloads

  @API @TCO @Components
  Scenario Outline: TCO components and their sources
    Given the TCO report has been generated
    Then the "<tco_component>" is sourced from "<architectural_source>"
    And it is measured using "<cost_metric_type>" cost metric

    Examples:
      | tco_component          | architectural_source          | cost_metric_type                |
      | AWS Egress (Networking)| Protocol Payload Parser       | Tiered Volume Data Transfer     |
      | ALB                    | Gateway Infrastructure        | Fixed Core + LCU Scaling        |
      | EC2 Replicas           | Scaled Compute Tier           | Sized Replica Node-Hours        |
      | FinOps Saving          | Programmatic Commitments      | Applied Discount Percentage     |
