package com.calculator.domain.dto.responses.containers;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.calculator.infrastructure.cloud.adapters.aws.containers.AWSContainersCostCalculatorAdapter.*;

public class ContainerPricingResponse {
    @JsonProperty
    public double eksControlPlanePerMonth = 73;
    @JsonProperty
    public double eksControlPlanePerHour = 0.10;
    @JsonProperty
    public double fargateVcpuPerHour = FARGATE_VCPU_PER_HOUR;
    @JsonProperty
    public double fargateGbPerHour = FARGATE_GB_PER_HOUR;
    @JsonProperty
    public double fargateSpotVcpuPerHour  = FARGATE_SPOT_VCPU_PER_HOUR;
    @JsonProperty
    public double fargateSpotGbPerHour = FARGATE_SPOT_GB_PER_HOUR;
    @JsonProperty
    public double ebsGp3PerGbMonth = EBS_GP3_PER_GB_MONTH;
    @JsonProperty
    public double ebsGp3IopsPerIopsMonth = EBS_GP3_IOPS_PER_IOPS_MONTH;
    @JsonProperty
    public double ebsGp3ThroughputPerMbpsMonth = EBS_GP3_THROUGHPUT_PER_MBPS;
    @JsonProperty
    public double albFixedPerMonth = ALB_FIXED_PER_MONTH;
    @JsonProperty
    public double albFixedPerHour = ALB_FIXED_PER_HOUR;
    @JsonProperty
    public double albLcuPerHour = ALB_LCU_PER_HOUR;
    @JsonProperty
    public double ecrStoragePerGbMonth = ECR_STORAGE_PER_GB_MONTH;
    @JsonProperty
    public double ecrDataTransferPerGb = ECR_DATA_TRANSFER_PER_GB;
    @JsonProperty
    public double wafWebAclPerMonth = WAF_WEB_ACL_PER_MONTH;
    @JsonProperty
    public double wafRulePerMonth = WAF_RULE_PER_MONTH;
    @JsonProperty
    public double wafPer1MRequests = WAF_PER_1M_REQUESTS;
    @JsonProperty
    public Map<String, Double> ec2OnDemandPrices= new LinkedHashMap<>();
    @JsonProperty
    public String source;
    @JsonProperty
    public String note;
}