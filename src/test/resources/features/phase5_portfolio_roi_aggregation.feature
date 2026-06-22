# Language: en
@ArchitectureCalculator @Phase5 @Portfolio
Feature: Multi-Service Portfolio TCO and ROI Aggregation
  As a Software Architect
  So that I can present the total financial footprint of a MACH microservice portfolio to Finance and Leadership
  I want to aggregate the TCO and ROI of all modeled services and download a portfolio PDF report

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @UI @Portfolio @Persistence
  Scenario: Portfolio persists across browser sessions
    Given the architect has added multiple services to the portfolio
    When the browser is refreshed or reopened
    Then all services are still present loaded from browser localStorage under the key "grpc_tco_portfolio_v1"
    And the portfolio can be cleared at any time via the Clear Portfolio action

  @API @Portfolio @ROI
  Scenario: Portfolio ROI is computed by the backend
    Given the portfolio contains one or more services
    When Phase 5 loads
    Then the tool calls POST /api/portfolio/roi with all service entries FinOps monthly savings and EC2 baseline spend
    And the backend returns aggregated metrics including total TCO total revenue portfolio ARPU monthly and annual ROI break-even users cost per request cost per user and FinOps-adjusted ROI

  @UI @Portfolio @PDFDownload
  Scenario: Portfolio PDF download supports stakeholder presentation
    Given the architect has modeled three microservices with a combined TCO
    When the Download Portfolio PDF button is clicked
    Then the browser print dialog opens with a clean print-optimized layout
    And the layout shows the full portfolio table with service names BUCs RPS monthly TCO and annual TCO
    And navigation elements and action buttons are hidden in the print view

  @API @Portfolio @UnitEconomics
  Scenario Outline: Portfolio aggregated unit economics
    Given a portfolio with "<services>" configured
    When the ROI aggregation is calculated
    Then total monthly TCO is "<total_tco_mo>"
    And total monthly revenue is "<total_revenue_mo>"
    And monthly ROI is "<monthly_roi>"

    Examples:
      | services                              | total_tco_mo | total_revenue_mo | monthly_roi                     |
      | 2 services, 10000 users               | $45.00       | $199.80          | +344%                           |
      | 3 services, 50000 users               | $312.00      | $4995.00         | +1502%                          |
      | 1 service, 500 users, no revenue      | $8.20        | —                | N/A (revenue not configured)    |
