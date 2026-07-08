package com.calculator.domain.dto.responses.containers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

public class ContainerTcoResponse {
    @JsonProperty
    public double eksControlPlaneCost = 0;
    @JsonProperty
    public double ec2NodesCost = 0;
    @JsonProperty
    public double fargateCost = 0;
    @JsonProperty
    public double hostStorageCost = 0;
    @JsonProperty
    public double backupStorageCost = 0;
    @JsonProperty
    public double pvcStorageCost = 0;
    @JsonProperty
    public double clusterLbCost = 0;
    @JsonProperty
    public double wafCost = 0;
    @JsonProperty
    public double ecrCost = 0;
    @JsonProperty
    public double hostOsLicenseCost = 0;
    @JsonProperty
    public double workloadLicenseCost = 0;
    @JsonProperty
    public double cronJobFargateCost = 0;
    @JsonProperty
    public double discountSaving = 0;
    @JsonProperty
    public double totalMonthlyContainerTco = 0;
    @JsonProperty
    public Map<String, String> lineItems = new LinkedHashMap<>();
    @JsonProperty
    public String ec2InstanceType;
    @JsonProperty
    public double ec2InstancePricePerHour;
    @JsonProperty
    public int estimatedNodeCount;
    @JsonProperty
    public String source;
    @JsonProperty
    public String note;
}