package com.calculator.domain.dto.requests.portfolio;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class PortfolioUnitEconomicsRequest {
    @JsonProperty
    public List<ServiceEntry> services = new ArrayList<>();
    @JsonProperty
    public double finopsMonthlySaving = 0.0;
    @JsonProperty
    public double ec2BaselineSpend = 0.0;
}
