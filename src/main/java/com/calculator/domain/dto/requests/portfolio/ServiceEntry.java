package com.calculator.domain.dto.requests.portfolio;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServiceEntry {
    @JsonProperty
    public String name;
    @JsonProperty
    public String buc;
    @JsonProperty
    public double tco; /* monthly TCO in USD */
    @JsonProperty
    public double revenuePerUserMonth; /* expected monthly revenue per end user */
    @JsonProperty
    public int consumers; /* end users for this service */
    @JsonProperty
    public int rps; /* requests per second */
    @JsonProperty
    public double finopsSaving; /* FinOps saving allocated to this service (optional) */
}
