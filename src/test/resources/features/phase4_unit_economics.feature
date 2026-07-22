@CostEfficiencyCalculator @Phase4 @UnitEconomics
Feature: Unit Economics report
  As a Software Architect
  So that I can evaluate the cost-efficiency of the microservice I modelled
  I want to calculate the unit economics

  Background:
    Given the architect is on the TCO Networking Costs Calculator interface

  @API @UnitEconomics @Calculation
  Scenario Outline: Validate Core Unit Economics Calculations with Positive and Negative ROI
    Given a unit economics request with networking cost "<egress>", cloud cost "<cloud>", RPS "<rps>", consumers "<consumers>", and ARPU "<arpu>"
    When the unit economics are calculated
    Then the response returns status 200
    And the calculated gross parameters match monthly TCO "<expected_monthly_tco>" and annual TCO "<expected_annual_tco>" with requests "<expected_requests>"
    And the unit cost parameters match cost per request "<expected_cost_per_req>", cost per user month "<expected_cost_user_month>", and cost per user day "<expected_cost_user_day>"
    And the ROI metrics match total monthly revenue "<expected_monthly_rev>", monthly profit "<expected_monthly_profit>", monthly ROI pct "<expected_roi_pct>", break-even users "<expected_breakeven>", and net margin per user "<expected_net_margin>"

    Examples:
      | egress  | cloud   | rps  | consumers | arpu | expected_monthly_tco  | expected_annual_tco | expected_requests | expected_cost_per_req | expected_cost_user_month | expected_cost_user_day | expected_monthly_rev | expected_monthly_profit | expected_roi_pct | expected_breakeven | expected_net_margin |
      | 1599.76 | 66.89   | 1000 | 350       | 7.0  | 1666.65               | 19999.80            | 2592000000        | 0.000001              | 4.7618                   | 0.158727               | 2450.00              | 783.35                  | 47.00            | 239                | 2.2382              |
      | 185.23  | 5257.97 | 1050 | 350       | 15.0 | 5443.20               | 65318.40            | 2721600000        | 0.000002              | 15.5520                  | 0.51840                | 5250.00              | -193.20                 | -3.55            | 363                | -0.5520             |