package com.calculator.domain.dto.responses.portfolio;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceRoiEntry {
    @JsonProperty
    public String name;
    @JsonProperty
    public String buc;
    @JsonProperty
    public double tco;
    @JsonProperty
    public double monthlyRevenue;
    @JsonProperty
    public double roi;
    @JsonProperty
    public double arpu;
    @JsonProperty
    public int consumers;
    @JsonProperty
    public int rps;
    @JsonProperty
    public double tcoShare; /* % of total portfolio TCO */
}
