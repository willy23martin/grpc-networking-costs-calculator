package com.calculator.domain.dto.requests;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CloudInfrastructureTotalCostRequest {
    @JsonProperty
    public double albMonthlyCostUsd = 0.0;
    @JsonProperty
    public double cacheMonthlyCostUsd = 0.0;
    @JsonProperty
    public double databaseMonthlyCostUsd = 0.0;
    @JsonProperty
    public double securityMonthlyCostUsd = 0.0;
    @JsonProperty
    public double containerMonthlyCostUsd = 0.0;
    @JsonProperty
    public double apiGatewayMonthlyCostUsd = 0.0;
    @JsonProperty
    public double ec2ReplicaMonthlyCostUsd = 0.0;
    @JsonProperty
    public double finopsMonthlySavingUsd = 0.0;
}