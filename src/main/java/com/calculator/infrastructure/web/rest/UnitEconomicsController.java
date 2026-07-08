package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.requests.CloudInfrastructureTotalCostRequest;
import com.calculator.domain.dto.requests.UnitEconomicsRequest;
import com.calculator.domain.dto.responses.CloudInfraTotalCostResponse;
import com.calculator.application.services.calculators.CostEfficiencyCalculator;
import com.calculator.domain.dto.responses.UnitEconomicsResponse;
import com.calculator.shared.JSONLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.logging.Logger;

import static com.calculator.application.services.utils.MathUtils.round2;

@RestController
@RequestMapping("/api/cost")
@CrossOrigin(origins = "*")
public class UnitEconomicsController {

    private static final Logger log = Logger.getLogger(UnitEconomicsController.class.getName());

    @Autowired
    private CostEfficiencyCalculator costEfficiencyCalculator;

    @PostMapping("/unit-economics")
    public ResponseEntity<UnitEconomicsResponse> calculateUnitEconomics(
            @RequestBody UnitEconomicsRequest unitEconomicsRequest) {

        UnitEconomicsResponse unitEconomicsResponse = costEfficiencyCalculator.calculateUnitEconomics(unitEconomicsRequest);
        unitEconomicsResponse.affordabilityImpact = costEfficiencyCalculator.summariseAffordabilityImpact().name();

        return ResponseEntity.ok(unitEconomicsResponse);
    }

    @PostMapping("/cloud-infra-total")
    public ResponseEntity<?> calculateCloudInfraTotalCosts(@RequestBody CloudInfrastructureTotalCostRequest cloudInfrastructureTotalCostRequest) {
        try {
            if (cloudInfrastructureTotalCostRequest == null) {
                return ResponseEntity.badRequest().body("Request body cannot be null");
            }

            log.info("CloudInfraTotalRequest received.");
            JSONLogger.logAsJSON(log, cloudInfrastructureTotalCostRequest);

            if (cloudInfrastructureTotalCostRequest.albMonthlyCostUsd < 0 || cloudInfrastructureTotalCostRequest.cacheMonthlyCostUsd < 0 ||
                    cloudInfrastructureTotalCostRequest.databaseMonthlyCostUsd < 0 || cloudInfrastructureTotalCostRequest.securityMonthlyCostUsd < 0 ||
                    cloudInfrastructureTotalCostRequest.containerMonthlyCostUsd < 0 || cloudInfrastructureTotalCostRequest.apiGatewayMonthlyCostUsd < 0 ||
                    cloudInfrastructureTotalCostRequest.ec2ReplicaMonthlyCostUsd < 0 || cloudInfrastructureTotalCostRequest.finopsMonthlySavingUsd < 0) {

                return ResponseEntity.badRequest().body("Cost and saving metrics must be non-negative values.");
            }

            CloudInfraTotalCostResponse cloudInfraTotalCostResponse = getCloudInfraTotalCostResponse(cloudInfrastructureTotalCostRequest);
            return ResponseEntity.ok(cloudInfraTotalCostResponse);

        } catch (IllegalArgumentException e) {
            log.warning("Validation error processing cloud infra total: " + e.getMessage());
            return ResponseEntity.badRequest().body("Invalid request data: " + e.getMessage());

        } catch (Exception e) {
            log.severe("Unexpected error calculating cloud infrastructure totals: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while processing cloud cost calculations.");
        }
    }

    private static CloudInfraTotalCostResponse getCloudInfraTotalCostResponse(CloudInfrastructureTotalCostRequest cloudInfrastructureTotalCostRequest) {
        CloudInfraTotalCostResponse cloudInfraTotalCostResponse = new CloudInfraTotalCostResponse();

        if (cloudInfraTotalCostResponse.perServiceBreakdown == null) {
            cloudInfraTotalCostResponse.perServiceBreakdown = new HashMap<>();
        }

        double grossCloudInfraCosts = cloudInfrastructureTotalCostRequest.albMonthlyCostUsd
                + cloudInfrastructureTotalCostRequest.cacheMonthlyCostUsd
                + cloudInfrastructureTotalCostRequest.databaseMonthlyCostUsd
                + cloudInfrastructureTotalCostRequest.securityMonthlyCostUsd
                + cloudInfrastructureTotalCostRequest.containerMonthlyCostUsd
                + cloudInfrastructureTotalCostRequest.apiGatewayMonthlyCostUsd
                + cloudInfrastructureTotalCostRequest.ec2ReplicaMonthlyCostUsd;

        double finOpsSavings = cloudInfrastructureTotalCostRequest.finopsMonthlySavingUsd;

        cloudInfraTotalCostResponse.grossCloudInfraCostUsd = round2(grossCloudInfraCosts);
        cloudInfraTotalCostResponse.finopsSavingUsd = round2(finOpsSavings);
        cloudInfraTotalCostResponse.netCloudInfraCostUsd = round2(Math.max(0, grossCloudInfraCosts - finOpsSavings));

        if (cloudInfrastructureTotalCostRequest.albMonthlyCostUsd > 0) {
            cloudInfraTotalCostResponse.perServiceBreakdown.put("ALB", round2(cloudInfrastructureTotalCostRequest.albMonthlyCostUsd));
        }
        if (cloudInfrastructureTotalCostRequest.cacheMonthlyCostUsd > 0) {
            cloudInfraTotalCostResponse.perServiceBreakdown.put("ElastiCache", round2(cloudInfrastructureTotalCostRequest.cacheMonthlyCostUsd));
        }
        if (cloudInfrastructureTotalCostRequest.databaseMonthlyCostUsd > 0) {
            cloudInfraTotalCostResponse.perServiceBreakdown.put("Database/Backup", round2(cloudInfrastructureTotalCostRequest.databaseMonthlyCostUsd));
        }
        if (cloudInfrastructureTotalCostRequest.securityMonthlyCostUsd   > 0) {
            cloudInfraTotalCostResponse.perServiceBreakdown.put("Security", round2(cloudInfrastructureTotalCostRequest.securityMonthlyCostUsd));
        }
        if (cloudInfrastructureTotalCostRequest.containerMonthlyCostUsd  > 0) {
            cloudInfraTotalCostResponse.perServiceBreakdown.put("Container", round2(cloudInfrastructureTotalCostRequest.containerMonthlyCostUsd));
        }
        if (cloudInfrastructureTotalCostRequest.apiGatewayMonthlyCostUsd > 0) {
            cloudInfraTotalCostResponse.perServiceBreakdown.put("API Gateway", round2(cloudInfrastructureTotalCostRequest.apiGatewayMonthlyCostUsd));
        }
        if (cloudInfrastructureTotalCostRequest.ec2ReplicaMonthlyCostUsd > 0) {
            cloudInfraTotalCostResponse.perServiceBreakdown.put("EC2 Replicas", round2(cloudInfrastructureTotalCostRequest.ec2ReplicaMonthlyCostUsd));
        }
        return cloudInfraTotalCostResponse;
    }

}