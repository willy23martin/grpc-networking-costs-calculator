package com.calculator.infrastructure.web.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class CloudServiceCostControllerTest {

    private final CloudServiceCostController controller = new CloudServiceCostController();

    @Test
    void calculateAlbCost_withAndWithoutLcu() {
        CloudServiceCostController.AlbCostRequest req1 = new CloudServiceCostController.AlbCostRequest();
        req1.albCount = 2;
        req1.lcuPerHour = 5.0;
        req1.fixedPerMonthUsd = 16.20;
        req1.lcuPerHourUsd = 0.008;

        ResponseEntity<CloudServiceCostController.ServiceCostResponse> response1 = controller.calculateAlbCost(req1);
        assertNotNull(response1.getBody());
        assertTrue(response1.getBody().monthlyTotalUsd > 0);
        assertEquals("2 ALBs", response1.getBody().summaryLabel);

        CloudServiceCostController.AlbCostRequest req2 = new CloudServiceCostController.AlbCostRequest();
        req2.albCount = 1;
        req2.lcuPerHour = 0.0;

        ResponseEntity<CloudServiceCostController.ServiceCostResponse> response2 = controller.calculateAlbCost(req2);
        assertNotNull(response2.getBody());
        assertEquals("1 ALB", response2.getBody().summaryLabel);
    }

    @Test
    void calculateDatabaseBackupCost_allBranches() {
        CloudServiceCostController.DatabaseBackupCostRequest req1 = new CloudServiceCostController.DatabaseBackupCostRequest();
        req1.s3BackupEnabled = true;
        req1.rdsSnapshotEnabled = true;
        req1.rdsMultiAzEnabled = true;
        req1.auroraReplicaEnabled = true;
        req1.dynamoGlobalEnabled = true;
        req1.databaseSizeGb = 50.0;
        req1.auroraReplicaCount = 2;
        req1.dynamoExtraRegions = 2;

        ResponseEntity<CloudServiceCostController.ServiceCostResponse> response1 = controller.calculateDatabaseBackupCost(req1);
        assertNotNull(response1.getBody());
        assertTrue(response1.getBody().monthlyTotalUsd > 0);

        CloudServiceCostController.DatabaseBackupCostRequest req2 = new CloudServiceCostController.DatabaseBackupCostRequest();
        ResponseEntity<CloudServiceCostController.ServiceCostResponse> response2 = controller.calculateDatabaseBackupCost(req2);
        assertNotNull(response2.getBody());
        assertEquals(0.0, response2.getBody().monthlyTotalUsd);
    }

    @Test
    void calculateSecurityServicesCost_allEnabled() {
        CloudServiceCostController.SecurityServicesCostRequest req = new CloudServiceCostController.SecurityServicesCostRequest();
        req.requestsPerSecond = 100;
        req.guardDutyEnabled = true;
        req.guardDutyLogsGb = 600.0;
        req.inspectorEnabled = true;
        req.inspectorInstanceCount = 5;
        req.wafEnabled = true;
        req.wafCustomRulesCount = 3;
        req.macieEnabled = true;
        req.macieS3DataGb = 10.0;
        req.cloudWatchEnabled = true;
        req.cloudWatchIngestGb = 5.0;
        req.auditManagerEnabled = true;
        req.auditActiveAssessments = 2;
        req.kmsEnabled = true;
        req.kmsCustomManagedKeyCount = 4;

        ResponseEntity<CloudServiceCostController.ServiceCostResponse> response = controller.calculateSecurityServicesCost(req);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().monthlyTotalUsd > 0);
    }

    @Test
    void calculateSecurityServicesCost_disabledAndEdgeCases() {
        CloudServiceCostController.SecurityServicesCostRequest req = new CloudServiceCostController.SecurityServicesCostRequest();
        req.requestsPerSecond = 0;
        req.guardDutyEnabled = true;
        req.guardDutyLogsGb = 100.0;
        req.macieEnabled = true;
        req.macieS3DataGb = 0.5;

        ResponseEntity<CloudServiceCostController.ServiceCostResponse> response = controller.calculateSecurityServicesCost(req);
        assertNotNull(response.getBody());
        assertEquals(0.0, response.getBody().monthlyTotalUsd);
    }

    @Test
    void calculateCachingCost_redisBranches() {
        CloudServiceCostController.CachingCostRequest req1 = new CloudServiceCostController.CachingCostRequest();
        req1.cacheEngine = "redis";
        req1.nodeType = "r6g.2xlarge";
        req1.nodeCount = 2;

        ResponseEntity<CloudServiceCostController.ServiceCostResponse> response1 = controller.calculateCachingCost(req1);
        assertNotNull(response1.getBody());

        CloudServiceCostController.CachingCostRequest req2 = new CloudServiceCostController.CachingCostRequest();
        req2.cacheEngine = "REDIS";
        req2.nodeType = "r6g.xlarge";
        controller.calculateCachingCost(req2);

        CloudServiceCostController.CachingCostRequest req3 = new CloudServiceCostController.CachingCostRequest();
        req3.cacheEngine = null;
        req3.nodeType = null;
        controller.calculateCachingCost(req3);
    }

    @Test
    void calculateCachingCost_memcachedBranches() {
        CloudServiceCostController.CachingCostRequest req1 = new CloudServiceCostController.CachingCostRequest();
        req1.cacheEngine = "memcached";
        req1.nodeType = "r6g.xlarge";
        controller.calculateCachingCost(req1);

        CloudServiceCostController.CachingCostRequest req2 = new CloudServiceCostController.CachingCostRequest();
        req2.cacheEngine = "memcached";
        req2.nodeType = "r6g.large";
        controller.calculateCachingCost(req2);
    }

    @Test
    void calculateApiGatewayCost_allTypes() {
        CloudServiceCostController.ApiGatewayCostRequest httpReq = new CloudServiceCostController.ApiGatewayCostRequest();
        httpReq.apiGatewayType = "http";
        httpReq.callsPerMonthMillions = 10.0;
        httpReq.cacheHourlyRate = 0.0;
        ResponseEntity<CloudServiceCostController.ServiceCostResponse> httpResp = controller.calculateApiGatewayCost(httpReq);
        assertTrue(httpResp.getBody().monthlyTotalUsd > 0);

        CloudServiceCostController.ApiGatewayCostRequest wsReq = new CloudServiceCostController.ApiGatewayCostRequest();
        wsReq.apiGatewayType = "websocket";
        wsReq.callsPerMonthMillions = 5.0;
        wsReq.cacheHourlyRate = 0.05;
        ResponseEntity<CloudServiceCostController.ServiceCostResponse> wsResp = controller.calculateApiGatewayCost(wsReq);
        assertTrue(wsResp.getBody().monthlyTotalUsd > 0);

        CloudServiceCostController.ApiGatewayCostRequest restReq = new CloudServiceCostController.ApiGatewayCostRequest();
        restReq.apiGatewayType = "rest";
        restReq.callsPerMonthMillions = 2.0;
        ResponseEntity<CloudServiceCostController.ServiceCostResponse> restResp = controller.calculateApiGatewayCost(restReq);
        assertTrue(restResp.getBody().monthlyTotalUsd > 0);

        CloudServiceCostController.ApiGatewayCostRequest defaultReq = new CloudServiceCostController.ApiGatewayCostRequest();
        defaultReq.apiGatewayType = "invalid_type_fallback";
        controller.calculateApiGatewayCost(defaultReq);
    }

    @Test
    void calculateContainerCost_eksBranches() {
        CloudServiceCostController.ContainerCostRequest req = new CloudServiceCostController.ContainerCostRequest();
        req.eksClusterEnabled = true;
        req.orchestrationType = "eks";
        req.eksClusterCount = 1;
        req.clusterLoadBalancerEnabled = true;
        req.clusterLoadBalancerCount = 2;
        req.hostStorageEnabled = true;
        req.podCount = 25;
        req.clusterBackupEnabled = true;
        req.hostLicenseEnabled = true;
        req.hostOsLicenseRatePerHour = 0.04;
        req.workloadLicenseEnabled = true;
        req.workloadLicenseMonthlyCost = 100.0;
        req.spotDiscountPercent = 10;
        req.cronJobsOnFargateEnabled = true;
        req.statefulSetsEnabled = true;

        ResponseEntity<CloudServiceCostController.ServiceCostResponse> response = controller.calculateContainerCost(req);
        assertNotNull(response.getBody());
        assertTrue(response.getBody().monthlyTotalUsd > 0);
    }

    @Test
    void calculateContainerCost_ecsBranches() {
        CloudServiceCostController.ContainerCostRequest req = new CloudServiceCostController.ContainerCostRequest();
        req.eksClusterEnabled = true;
        req.orchestrationType = "ecs";
        ResponseEntity<CloudServiceCostController.ServiceCostResponse> response = controller.calculateContainerCost(req);
        assertNotNull(response.getBody());
    }

    @Test
    void calculateFinOpsDiscount_allScenarios() {
        CloudServiceCostController.FinOpsDiscountRequest req1 = new CloudServiceCostController.FinOpsDiscountRequest();
        req1.onDemandMonthlySpend = 1000.0;
        req1.standardRi1yrEnabled = true;
        req1.standardRi3yrEnabled = true;
        req1.convertibleRi1yrEnabled = true;
        req1.savingsPlan1yrEnabled = true;
        req1.savingsPlan3yrEnabled = true;
        req1.trustedAdvisorEnabled = true;
        req1.businessSupportMinMonthUsd = 50.0;
        req1.businessSupportPctMonthlyUsage = 15.0;

        ResponseEntity<CloudServiceCostController.FinOpsDiscountResponse> response1 = controller.calculateFinOpsDiscount(req1);
        assertNotNull(response1.getBody());
        assertTrue(response1.getBody().bestMonthlySavingUsd > 0);

        CloudServiceCostController.FinOpsDiscountRequest req2 = new CloudServiceCostController.FinOpsDiscountRequest();
        req2.onDemandMonthlySpend = 500.0;
        req2.trustedAdvisorEnabled = true;
        req2.businessSupportMinMonthUsd = 200.0;
        req2.businessSupportPctMonthlyUsage = 2.0;

        ResponseEntity<CloudServiceCostController.FinOpsDiscountResponse> response2 = controller.calculateFinOpsDiscount(req2);
        assertNotNull(response2.getBody());
    }
}